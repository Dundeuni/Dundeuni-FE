package com.dundueni.app.feature.analysis

import com.dundueni.app.BuildConfig
import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.remote.api.FakeAnalysisApiService
import com.dundueni.app.data.remote.network.RetrofitClient
import com.dundueni.app.data.repository.AnalysisRepository
import com.dundueni.app.data.repository.AnalysisResultRepository
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

/** One API mode shared by the same Repository, Domain, and ViewModel pipeline. */
object AnalysisFlowDependencies {
    /** Uses local.properties values generated into BuildConfig; unset configuration defaults to Fake. */
    fun createRepository(): AnalysisResultRepository =
        createRepository(BuildConfig.ANALYSIS_API_MODE, BuildConfig.ANALYSIS_API_BASE_URL)

    /** Exposed for deterministic tests and explicit local composition. */
    fun createRepository(mode: String, baseUrl: String? = null): AnalysisResultRepository {
        val service = try {
            when (mode.trim().uppercase()) {
                FAKE_MODE -> FakeAnalysisApiService()
                REAL_MODE -> RetrofitClient(validateBaseUrl(baseUrl)).analysisApiService
                else -> throw AnalysisApiConfigurationException(
                    "Unknown analysis.api.mode '$mode'. Set it to FAKE or REAL in local.properties."
                )
            }
        } catch (error: AnalysisApiConfigurationException) {
            ConfigurationErrorApiService(error)
        } catch (error: IllegalArgumentException) {
            ConfigurationErrorApiService(
                AnalysisApiConfigurationException(
                    "Invalid analysis.api.baseUrl. Use a valid HTTP(S) base URL ending with '/'.",
                    error
                )
            )
        }
        return AnalysisRepository(service)
    }

    private fun validateBaseUrl(baseUrl: String?): String {
        if (baseUrl.isNullOrBlank()) {
            throw AnalysisApiConfigurationException(
                "Real API mode requires analysis.api.baseUrl in local.properties."
            )
        }
        if (!baseUrl.trim().endsWith("/")) {
            throw AnalysisApiConfigurationException(
                "Invalid analysis.api.baseUrl. The HTTP(S) base URL must end with '/'."
            )
        }
        val parsed = baseUrl.trim().toHttpUrlOrNullCompat()
            ?: throw AnalysisApiConfigurationException(
                "Invalid analysis.api.baseUrl. Use a valid HTTP(S) base URL ending with '/'."
            )
        if (parsed.scheme !in SUPPORTED_SCHEMES || parsed.host.isBlank() ||
            !parsed.encodedPath.endsWith("/") || parsed.encodedUsername.isNotEmpty() ||
            parsed.encodedPassword.isNotEmpty() || parsed.encodedQuery != null || parsed.encodedFragment != null
        ) {
            throw AnalysisApiConfigurationException(
                "Invalid analysis.api.baseUrl. Use a valid HTTP(S) base URL ending with '/' and no credentials, query, or fragment."
            )
        }
        return parsed.toString()
    }

    private fun String.toHttpUrlOrNullCompat(): HttpUrl? =
        runCatching { toHttpUrl() }.getOrNull()

    private const val FAKE_MODE = "FAKE"
    private const val REAL_MODE = "REAL"
    private val SUPPORTED_SCHEMES = setOf("http", "https")
}

/** Configuration errors travel through the normal request path and become the existing UI Error state. */
class AnalysisApiConfigurationException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

private class ConfigurationErrorApiService(
    private val configurationError: AnalysisApiConfigurationException
) : AnalysisApiService {
    override suspend fun analyzeImage(
        image: MultipartBody.Part,
        type: RequestBody
    ): Response<ResponseBody> = throw configurationError
}
