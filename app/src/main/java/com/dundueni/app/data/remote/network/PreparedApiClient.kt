package com.dundueni.app.data.remote.network

import com.dundueni.app.data.auth.AuthSessionStore
import com.dundueni.app.data.remote.api.AnalysisSupportApiService
import com.dundueni.app.data.remote.api.HistoryApiService
import com.dundueni.app.data.remote.api.HomeDraftApiService
import com.dundueni.app.data.remote.api.UserSettingsApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Explicitly constructed only after a BE base URL and authenticated session are available. */
class PreparedApiClient(
    baseUrl: String,
    sessionStore: AuthSessionStore,
    onUnauthorized: (String) -> Unit = {},
) {
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(sessionStore, onUnauthorized))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(360, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val history: HistoryApiService by lazy { retrofit.create(HistoryApiService::class.java) }
    val userSettings: UserSettingsApiService by lazy { retrofit.create(UserSettingsApiService::class.java) }
    val home: HomeDraftApiService by lazy { retrofit.create(HomeDraftApiService::class.java) }
    val analysisSupport: AnalysisSupportApiService by lazy { retrofit.create(AnalysisSupportApiService::class.java) }
}
