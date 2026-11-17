package com.dundueni.app.feature.analysis

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.repository.AnalysisRepository
import com.dundueni.app.feature.analysisresult.RiskLevel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class MockAnalysisFlowTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    private val processor = AnalysisInputProcessor(RuntimeEnvironment.getApplication().contentResolver, RuntimeEnvironment.getApplication().cacheDir)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun photoAndCaptureFileReachViewModelThroughRealRetrofitWithoutInventingSafetyVerdicts() = runTest {
        val server = MockWebServer()
        server.start()
        val file = File.createTempFile("retrofit-image", ".png", RuntimeEnvironment.getApplication().cacheDir)
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            for (capture in listOf(false, true)) {
                server.enqueue(MockResponse().setResponseCode(202).setBody("""{
                    "isSuccess":true,"code":"ANALYSIS202","result":{
                        "analysisId":"server-id","type":"IMAGE","status":"COMPLETED",
                        "aiGenerationScore":82.4,"aiRiskLevel":"CAUTION",
                        "reasons":[{"description":"server AI finding"}]
                    }
                }"""))
                val vm = AnalysisViewModel(AnalysisFlowDependencies.createRepository("REAL", server.url("/").toString()))
                store.put("analysis", vm)
                val completed = async { vm.uiState.first { it is AnalysisUiState.Success || it is AnalysisUiState.Error } }
                vm.analyzePrepared {
                    if (capture) processor.prepareCaptureFile(file)
                    else processor.prepare(ImageInput.PhotoPicker(Uri.fromFile(file)))
                }
                val result = (completed.await() as AnalysisUiState.Success).result
                assertEquals("server-id", result.analysisId)
                assertEquals("CAUTION", result.aiRiskLevel)
                assertEquals(listOf("server AI finding"), result.reasons)
                val realPresentation = checkNotNull(result.toPb05Result(isDemo = false))
                assertEquals(RiskLevel.UNKNOWN, realPresentation.riskLevel)
                assertNull(realPresentation.fraudLevel)
                val request = server.takeRequest()
                assertEquals("/api/analysis", request.path)
                val body = request.body.readUtf8()
                assertTrue(body.contains("name=\"type\""))
                assertTrue(body.contains("IMAGE"))
                assertTrue(body.contains("name=\"file\""))
                assertTrue(body.contains("Content-Type: image/png"))
            }
        } finally {
            bitmap.recycle()
            file.delete()
            server.shutdown()
        }
    }

    @Test fun invalidResponsesLeaveLoadingAndReachError() = runTest {
        val responses = listOf(
            Response.success<ResponseBody>(null),
            Response.success("".toResponseBody()),
            Response.success("null".toResponseBody()),
            Response.success("""{"reasons":{}}""".toResponseBody()),
            Response.success("{}".toResponseBody()),
            Response.success("""{"risk_level":"INVALID"}""".toResponseBody()),
            Response.error<ResponseBody>(500, "error".toResponseBody())
        )
        val part = MultipartBody.Part.createFormData("file", "test.png", byteArrayOf(1).toRequestBody())
        for (response in responses) {
            val responseGate = CompletableDeferred<Unit>()
            val api = object : AnalysisApiService {
                override suspend fun analyzeImage(image: MultipartBody.Part, type: RequestBody): Response<ResponseBody> {
                    responseGate.await()
                    return response
                }
            }
            val vm = AnalysisViewModel(AnalysisRepository(api))
            store.put("analysis", vm)
            val states = mutableListOf<AnalysisUiState>()
            val observedTerminal = CompletableDeferred<Unit>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.uiState.collect {
                    states.add(it)
                    if (it is AnalysisUiState.Error || it is AnalysisUiState.Success) observedTerminal.complete(Unit)
                }
            }
            val completed = async { vm.uiState.first { it is AnalysisUiState.Error || it is AnalysisUiState.Success } }
            vm.analyze(part)
            assertEquals(AnalysisUiState.Loading, vm.uiState.value)
            responseGate.complete(Unit)
            val error = AnalysisUiState.Error(AnalysisErrorKind.HTTP, response.code())
            assertEquals(error, completed.await())
            observedTerminal.await()
            assertEquals(listOf(AnalysisUiState.Idle, AnalysisUiState.Loading, error), states)
            collector.cancel()
        }
    }

    @Test fun realHttpFailureRetriesSameCaptureAndAcceptsNullableResultFields() = runTest {
        val server = MockWebServer()
        server.start()
        val file = File.createTempFile("retry-capture", ".png", RuntimeEnvironment.getApplication().cacheDir)
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val bytes = file.readBytes()
            server.enqueue(MockResponse().setResponseCode(503).setBody("""{"isSuccess":false,"code":"UNAVAILABLE"}"""))
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{
                "isSuccess":true,"code":"ANALYSIS202","result":{
                    "status":"COMPLETED","analysisId":null,"type":null,
                    "aiGenerationScore":null,"aiRiskLevel":null,"reasons":null
                }
            }"""))
            val vm = AnalysisViewModel(AnalysisFlowDependencies.createRepository("REAL", server.url("/").toString()))
            store.put("analysis", vm)
            val prepare: suspend () -> MultipartBody.Part = { processor.prepareCaptureFile(file) }
            vm.analyzePrepared(prepare)
            assertEquals(AnalysisUiState.Error(AnalysisErrorKind.HTTP, 503),
                vm.uiState.first { it is AnalysisUiState.Error || it is AnalysisUiState.Success })
            runCurrent()
            vm.analyzePrepared(prepare)
            val success = vm.uiState.first { it is AnalysisUiState.Success || it is AnalysisUiState.Error } as AnalysisUiState.Success
            assertNull(success.result.analysisId)
            assertNull(success.result.aiGenerationScore)
            assertNull(success.result.aiRiskLevel)
            assertTrue(success.result.reasons.isEmpty())
            assertEquals(RiskLevel.UNKNOWN, success.result.toPb05Result(isDemo = false)?.riskLevel)
            repeat(2) {
                val request = checkNotNull(server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS))
                assertEquals("POST", request.method)
                assertEquals("/api/analysis", request.path)
                val body = request.body.readByteArray().toString(Charsets.ISO_8859_1)
                assertTrue(body.contains("name=\"type\""))
                assertTrue(body.contains("IMAGE"))
                assertTrue(body.contains("name=\"file\"; filename=\"capture.png\""))
                assertTrue(body.contains(bytes.toString(Charsets.ISO_8859_1)))
            }
            assertArrayEquals(bytes, file.readBytes())
            assertEquals(2, server.requestCount)
        } finally {
            bitmap.recycle()
            file.delete()
            server.shutdown()
        }
    }

    @Test fun malformedRealResponseBecomesResponseErrorWithoutFakeFallback() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(202).setBody("{broken JSON"))
            val vm = AnalysisViewModel(AnalysisFlowDependencies.createRepository("REAL", server.url("/").toString()))
            store.put("analysis", vm)
            vm.analyze(MultipartBody.Part.createFormData("file", "image.png", byteArrayOf(1).toRequestBody()))
            assertEquals(AnalysisUiState.Error(AnalysisErrorKind.RESPONSE),
                vm.uiState.first { it is AnalysisUiState.Error || it is AnalysisUiState.Success })
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }
}
