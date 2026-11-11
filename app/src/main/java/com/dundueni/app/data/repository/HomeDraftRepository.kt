package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.HomeDraftApiService
import com.dundueni.app.data.remote.dto.HomeDashboardDto
import com.dundueni.app.data.remote.dto.SafetyTipsDto

/** No home screen or floating-button state is changed by these draft calls. */
class HomeDraftRepository(private val api: HomeDraftApiService) {
    suspend fun dashboard(): HomeDashboardDto {
        val response = api.dashboard()
        val result = response.requireData(200, response.body()?.success, response.body()?.data)
        checked(result.floating?.enabled != null && result.recentAnalyses != null,
            "Incomplete home dashboard")
        return result
    }

    suspend fun safetyTips(limit: Int? = null): SafetyTipsDto {
        require(limit == null || limit in 1..10)
        val response = api.safetyTips(limit)
        val result = response.requireData(200, response.body()?.success, response.body()?.data)
        checked(result.tips != null, "Missing safety tips")
        return result
    }
}
