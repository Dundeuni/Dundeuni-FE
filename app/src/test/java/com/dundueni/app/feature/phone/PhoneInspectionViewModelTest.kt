package com.dundueni.app.feature.phone

import com.dundueni.app.data.auth.AuthSession
import com.dundueni.app.data.auth.AuthSessionSource
import com.dundueni.app.data.auth.AuthSessionStore
import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.remote.api.PhoneAnalysisApiService
import com.dundueni.app.data.remote.api.PhoneAnalysisRequestDto
import com.dundueni.app.data.remote.api.PhoneAnalysisResponseDto
import com.dundueni.app.data.remote.network.RetrofitClient
import com.dundueni.app.data.repository.HistoryApiRepository
import com.dundueni.app.feature.analysis.ScreenResultType
import com.dundueni.app.feature.analysisresult.AnalysisResult
import com.dundueni.app.feature.analysisresult.AnalysisResultActivity
import com.dundueni.app.feature.analysisresult.AnalysisResultJsonAdapter
import com.dundueni.app.feature.analysisresult.RiskLevel
import com.dundueni.app.feature.analysisresult.selectablePhoneNumbers
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.feature.payment.BasicInspectionQuota
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class PhoneInspectionViewModelTest {
    private val selected = ExtractedPhoneNumber("021234567", "02-123-4567", "TEXT")
    private val other = ExtractedPhoneNumber("01012345678", "010-1234-5678", "TEXT")
    private val origin = AnalysisResult(
        "text-id", RiskLevel.CAUTION, "", historyType = HistoryType.TEXT,
        extractedPhoneNumbers = listOf(other, selected),
    )

    @Test fun selectionOnlyIncludesTextAndOcrAndPreservesOrderAndDisplayValues() {
        val numbers = List(12) { index ->
            ExtractedPhoneNumber("010123456${"%02d".format(index)}", "표시-$index", "OCR")
        }
        val screen = origin.copy(historyType = HistoryType.SCREEN, extractedPhoneNumbers = numbers)
        assertEquals(listOf(other, selected), origin.selectablePhoneNumbers())
        assertEquals("02-123-4567", origin.selectablePhoneNumbers()[1].displayNumber)
        assertEquals(numbers.take(10), screen.selectablePhoneNumbers())
        assertEquals(numbers.take(10), screen.selectablePhoneNumbers(ScreenResultType.TEXT))
        assertTrue(screen.selectablePhoneNumbers(ScreenResultType.IMAGE).isEmpty())
        assertTrue(origin.copy(extractedPhoneNumbers = emptyList()).selectablePhoneNumbers().isEmpty())
        for (type in listOf(HistoryType.IMAGE, HistoryType.PHONE)) {
            assertTrue(origin.copy(historyType = type).selectablePhoneNumbers().isEmpty())
        }
    }

    @Test fun selectedPhoneUsesBearerPostThenRetriesOnlyStoredDetailAndDeliversThePhoneResultAtZeroQuota() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            MockWebServer().use { server ->
                server.start()
                val application = RuntimeEnvironment.getApplication()
                val quota = BasicInspectionQuota(application)
                repeat(BasicInspectionQuota.LIMIT) { assertTrue(quota.recordCompleted("test-image-$it")) }
                assertEquals(0, quota.remaining())
                val expiry = System.currentTimeMillis() / 1000 + 600
                val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"exp":$expiry}""".toByteArray())
                val session = AuthSession("e30.$payload.test", AuthSessionSource.BACKEND)
                val store = object : AuthSessionStore {
                    private var current: AuthSession? = session
                    override fun read() = current
                    override fun save(session: AuthSession) { current = session }
                    override fun clear() { current = null }
                }
                val client = RetrofitClient(server.url("/").toString(), store)
                val model = PhoneInspectionViewModel.forExtractedPhones(
                    application, { client.phoneAnalysisApiService }, { HistoryApiRepository(client.historyApiService) },
                )
                val analysisId = "d734f69e-ce2e-4914-8a2e-230713d37c96"
                val detailPath = "/api/analysis/history/$analysisId"
                server.enqueue(MockResponse().setResponseCode(201).setBody(
                    """{"analysisId":"$analysisId","status":"COMPLETED"}""",
                ))
                server.enqueue(MockResponse().setResponseCode(503).setBody("""{"code":"ANALYSIS503"}"""))
                server.enqueue(MockResponse().setResponseCode(200).setBody("""{
                    "isSuccess":true,"code":"ANALYSIS200","result":{
                      "analysisId":"$analysisId","type":"PHONE","status":"COMPLETED",
                      "createdAt":"2026-11-25T06:30:00Z",
                      "detail":{"maskedPhoneNumber":"02-***-4567","riskLevel":"CAUTION",
                        "reportCount":2,"reasons":["서버 판단 근거"]}}}"""))

                assertEquals(0, server.requestCount)
                model.inspect(origin.selectablePhoneNumbers()[1].phoneNumber)
                val failure = withTimeout(5_000) { model.uiState.filterIsInstance<PhoneInspectionUiState.Error>().first() }
                assertTrue(failure.message.contains("HTTP 503"))
                assertEquals(listOf(other, selected), origin.extractedPhoneNumbers)
                val post = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
                assertEquals("/api/analyses/phone", post.path)
                assertEquals("POST", post.method)
                assertEquals("""{"phoneNumber":"021234567"}""", post.body.readUtf8())
                assertEquals("Bearer ${session.accessToken}", post.getHeader("Authorization"))
                assertEquals(detailPath, server.takeRequest(5, TimeUnit.SECONDS)?.path)

                model.retry()
                val success = withTimeout(5_000) { model.uiState.filterIsInstance<PhoneInspectionUiState.Success>().first() }
                val get = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))
                assertEquals("GET", get.method)
                assertEquals(detailPath, get.path)
                assertEquals("Bearer ${session.accessToken}", get.getHeader("Authorization"))
                assertEquals(3, server.requestCount)
                assertEquals(RiskLevel.CAUTION, success.result.riskLevel)
                assertEquals("02-***-4567", success.result.targetLabel)
                assertEquals(listOf("서버 판단 근거"), success.result.evidence.map { it.description })

                val destination = AnalysisResultActivity.createPhoneResultIntent(application, success.result)
                val delivered = checkNotNull(AnalysisResultActivity.readDelivery(application, destination)?.resultJson)
                assertEquals(success.result, AnalysisResultJsonAdapter.decode(delivered))
                assertEquals(AnalysisResultActivity::class.java.name, destination.component?.className)
                assertEquals(0, quota.remaining())
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun loadingBlocksAnotherNumberAndRetryKeepsTheSelectionUntilCancelled() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val calls = mutableListOf<String>()
            val first = CompletableDeferred<Response<PhoneAnalysisResponseDto>>()
            val second = CompletableDeferred<Response<PhoneAnalysisResponseDto>>()
            val model = PhoneInspectionViewModel.forExtractedPhones(
                RuntimeEnvironment.getApplication(),
                phoneApi = {
                    object : PhoneAnalysisApiService {
                        override suspend fun analyzePhone(request: PhoneAnalysisRequestDto): Response<PhoneAnalysisResponseDto> {
                            calls += request.phoneNumber
                            return if (calls.size == 1) first.await() else second.await()
                        }
                    }
                },
                history = { error("A failed POST has no detail to fetch") },
            )
            model.inspect(selected.phoneNumber)
            model.inspect(other.phoneNumber)
            runCurrent()
            assertEquals(PhoneInspectionUiState.Loading, model.uiState.value)
            assertEquals(listOf(selected.phoneNumber), calls)

            first.complete(Response.error(429, "{}".toResponseBody()))
            runCurrent()
            assertTrue((model.uiState.value as PhoneInspectionUiState.Error).message.contains("429"))
            model.retry()
            runCurrent()
            assertEquals(listOf(selected.phoneNumber, selected.phoneNumber), calls)
            model.cancel()
            second.complete(Response.success(201, PhoneAnalysisResponseDto("d734f69e-ce2e-4914-8a2e-230713d37c96", "COMPLETED")))
            runCurrent()
            assertEquals(PhoneInspectionUiState.Idle, model.uiState.value)
        } finally { Dispatchers.resetMain() }
    }
}
