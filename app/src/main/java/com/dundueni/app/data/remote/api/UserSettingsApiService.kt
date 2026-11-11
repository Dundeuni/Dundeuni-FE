package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.dto.OnboardingRequestDto
import com.dundueni.app.data.remote.dto.PreparedApiEnvelope
import com.dundueni.app.data.remote.dto.SettingsPatchRequestDto
import com.dundueni.app.data.remote.dto.UsageDto
import com.dundueni.app.data.remote.dto.UserSettingsDto
import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.PUT

/** Prepared independently of the current local onboarding, settings and quota. */
interface UserSettingsApiService {
    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @PUT("/api/me/onboarding")
    suspend fun completeOnboarding(@Body request: OnboardingRequestDto): Response<PreparedApiEnvelope<UserSettingsDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/me/settings")
    suspend fun getSettings(): Response<PreparedApiEnvelope<UserSettingsDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @PATCH("/api/me/settings")
    suspend fun updateSettings(@Body request: SettingsPatchRequestDto): Response<PreparedApiEnvelope<UserSettingsDto>>

    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/me/usage")
    suspend fun getUsage(): Response<PreparedApiEnvelope<UsageDto>>
}
