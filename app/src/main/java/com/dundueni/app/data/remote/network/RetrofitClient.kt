package com.dundueni.app.data.remote.network

import com.dundueni.app.data.auth.AuthSessionStore
import com.dundueni.app.data.remote.api.AnalysisApiService
import com.dundueni.app.data.remote.api.AnalysisQueryApiService
import com.dundueni.app.data.remote.api.TextAnalysisApiService
import com.dundueni.app.data.remote.api.AuthApiService
import com.dundueni.app.data.remote.api.PhoneAnalysisApiService
import com.dundueni.app.data.remote.api.PhotoSelectionApiService
import com.dundueni.app.data.remote.api.ServerHistoryApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 호출 측에서 확정된 BE base URL을 전달하고, 생성한 인스턴스를 재사용합니다.
 * [baseUrl]은 마지막이 '/'인 HTTP/HTTPS URL이어야 합니다.
 */
/**
 * 서버 기본 주소를 받아 실제 통신용 AnalysisApiService를 만드는 객체다.
 * OkHttp가 HTTP 통신을 담당하고 Retrofit이 API 인터페이스를 호출로 연결한다.
 * Gson 변환기도 등록하지만 현재 API는 원문 ResponseBody를 반환한다.
 * 따라서 현재 결과 JSON의 해석은 AnalysisRepository가 직접 수행한다.
 * 앱의 기본 조립은 아직 Fake를 사용하며, 이 객체는 실제 API 연결에 사용한다.
 */
class RetrofitClient(
    baseUrl: String,
    sessionStore: AuthSessionStore? = null,
    onUnauthorized: (String) -> Unit = {},
) {
    private val okHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(BearerAuthInterceptor(sessionStore ?: EmptyAuthSessionStore, onUnauthorized))
        connectTimeout(15, TimeUnit.SECONDS)
        readTimeout(300, TimeUnit.SECONDS)
        writeTimeout(60, TimeUnit.SECONDS)
        callTimeout(360, TimeUnit.SECONDS)
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val analysisApiService: AnalysisApiService by lazy {
        retrofit.create(AnalysisApiService::class.java)
    }

    val analysisQueryApiService: AnalysisQueryApiService by lazy {
        retrofit.create(AnalysisQueryApiService::class.java)
    }

    val textAnalysisApiService: TextAnalysisApiService by lazy {
        retrofit.create(TextAnalysisApiService::class.java)
    }

    val historyApiService: ServerHistoryApiService by lazy {
        retrofit.create(ServerHistoryApiService::class.java)
    }

    val phoneAnalysisApiService: PhoneAnalysisApiService by lazy {
        retrofit.create(PhoneAnalysisApiService::class.java)
    }

    val photoSelectionApiService: PhotoSelectionApiService by lazy {
        retrofit.create(PhotoSelectionApiService::class.java)
    }

    // Public authentication calls neither attach a session nor invalidate it on login failure.
    // Disable implicit replay of one-use verification/signup requests and cross-origin redirects.
    val authApiService: AuthApiService by lazy {
        retrofit.newBuilder().client(okHttpClient.newBuilder()
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build())
            .build().create(AuthApiService::class.java)
    }
}

private object EmptyAuthSessionStore : AuthSessionStore {
    override fun read() = null
    override fun save(session: com.dundueni.app.data.auth.AuthSession) = Unit
    override fun clear() = Unit
}
