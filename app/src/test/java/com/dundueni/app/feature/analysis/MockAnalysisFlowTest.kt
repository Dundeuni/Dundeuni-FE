package com.dundueni.app.feature.analysis

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.remote.api.FakeAnalysisApiService
import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.repository.AnalysisRepository
import com.dundueni.app.feature.analysisresult.RiskLevel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

    @Test fun bothInputsReachSuccessThroughRealRepositoryAndMapper() = runTest {
        val file = File.createTempFile("mock-image", ".png", RuntimeEnvironment.getApplication().cacheDir)
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            for (level in RiskLevel.entries) {
              for (input in listOf(ImageInput.PhotoPicker(Uri.fromFile(file)), ImageInput.ScreenCapture(bitmap))) {
                val vm = AnalysisViewModel(AnalysisRepository(FakeAnalysisApiService("MOCK_${level.name}")))
                store.put("analysis", vm)
                assertEquals(AnalysisUiState.Idle, vm.uiState.value)
                val completed = async { vm.uiState.filter { it is AnalysisUiState.Success || it is AnalysisUiState.Error }.first() }
                vm.analyzePrepared { processor.prepare(input) }
                assertEquals(AnalysisUiState.Loading, vm.uiState.value)
                val state = completed.await()
                assertTrue(state is AnalysisUiState.Success)
                val result = (state as AnalysisUiState.Success).result
                assertEquals("MOCK_${level.name}", result.aiRiskLevel)
                val presented = checkNotNull(result.toPb05Result(isDemo = true))
                assertEquals(level, presented.riskLevel)
                assertEquals(result.analysisId, presented.id)
                assertEquals(result.reasons.single(), presented.evidence.single().description)
                assertNull(presented.fraudScore)
                assertEquals("Mock test data: verify the source before sharing.", result.reasons.single())
                assertTrue(result.recommendedActions.isEmpty())
              }
            }
            // 실제 화면 캡처의 Activity 간 전달 경로도 같은 Factory/Fake/Mapper로 검증합니다.
            val captureVm = AnalysisViewModel(AnalysisFlowDependencies.createRepository())
            store.put("analysis", captureVm)
            val completed = async { captureVm.uiState.filter { it is AnalysisUiState.Success || it is AnalysisUiState.Error }.first() }
            captureVm.analyzePrepared { processor.prepareCaptureFile(file) }
            assertEquals(AnalysisUiState.Loading, captureVm.uiState.value)
            val captureResult = completed.await() as AnalysisUiState.Success
            assertEquals(RiskLevel.CAUTION, captureResult.result.toPb05Result(isDemo = true)?.riskLevel)
        } finally {
            bitmap.recycle()
            file.delete()
        }
    }

    @Test fun unreadableInputReachesErrorWithoutServer() = runTest {
        val vm = AnalysisViewModel(AnalysisRepository(FakeAnalysisApiService()))
        store.put("analysis", vm)
        val completed = async { vm.uiState.filter { it == AnalysisUiState.Error }.first() }
        vm.analyzePrepared { processor.prepare(ImageInput.PhotoPicker(Uri.parse("file:///missing-flow-image"))) }
        assertEquals(AnalysisUiState.Loading, vm.uiState.value)
        assertEquals(AnalysisUiState.Error, completed.await())
    }

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
                assertNull(result.toPb05Result(isDemo = false))
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
            val api = object : AnalysisApiService {
                override suspend fun analyzeImage(image: MultipartBody.Part, type: RequestBody) = response
            }
            val vm = AnalysisViewModel(AnalysisRepository(api))
            store.put("analysis", vm)
            val states = mutableListOf<AnalysisUiState>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.toList(states) }
            val completed = async { vm.uiState.first { it == AnalysisUiState.Error || it is AnalysisUiState.Success } }
            vm.analyze(part)
            assertEquals(AnalysisUiState.Error, completed.await())
            assertEquals(listOf(AnalysisUiState.Idle, AnalysisUiState.Loading, AnalysisUiState.Error), states)
            collector.cancel()
        }
    }

    @Test fun mockCallFailureLeavesLoadingAndReachesError() = runTest {
        val vm = AnalysisViewModel(AnalysisRepository(FakeAnalysisApiService()))
        store.put("analysis", vm)
        val completed = async { vm.uiState.first { it == AnalysisUiState.Error || it is AnalysisUiState.Success } }
        // Fake가 빈 이미지 본문을 읽은 뒤 직접 예외를 발생시키는 경로입니다.
        val part = MultipartBody.Part.createFormData("file", "empty.png", byteArrayOf().toRequestBody())
        vm.analyze(part)
        assertEquals(AnalysisUiState.Error, completed.await())
        assertEquals(AnalysisUiState.Error, vm.uiState.value)
    }
}
