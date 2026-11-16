package com.dundueni.app.data.remote.api

import com.dundueni.app.data.remote.dto.HomeDashboardResponseDto
import com.dundueni.app.data.remote.dto.SafetyTipsResponseDto
import com.dundueni.app.data.remote.network.BearerAuthInterceptor
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/** Draft home APIs are kept separate from the working local home screen. */
interface HomeDraftApiService {
    @Headers("${BearerAuthInterceptor.REQUIRES_AUTH_HEADER}: true")
    @GET("/api/home")
    suspend fun dashboard(): Response<HomeDashboardResponseDto>

    @GET("/api/safety-tips")
    suspend fun safetyTips(@Query("limit") limit: Int? = null): Response<SafetyTipsResponseDto>

    // PATCH floating is withheld: the ZIP disagrees on /user/me and /users/me.
}
