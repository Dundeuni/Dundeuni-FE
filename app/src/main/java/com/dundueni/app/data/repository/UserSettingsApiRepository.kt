package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.UserSettingsApiService
import com.dundueni.app.data.remote.dto.OnboardingRequestDto
import com.dundueni.app.data.remote.dto.SettingsPatchRequestDto
import com.dundueni.app.data.remote.dto.UsageDto
import com.dundueni.app.data.remote.dto.UserSettingsDto

/** Prepared only; existing on-device preferences and quota remain authoritative in the app. */
class UserSettingsApiRepository(private val api: UserSettingsApiService) {
    suspend fun completeOnboarding(easyMode: Boolean, ageRange: String? = null): UserSettingsDto {
        require(ageRange == null || ageRange.length <= 20)
        return api.completeOnboarding(OnboardingRequestDto(easyMode, ageRange))
            .requireResult(200, "SETTINGS200").also(::validateSettings)
    }

    suspend fun getSettings(): UserSettingsDto =
        api.getSettings().requireResult(200, "SETTINGS200").also(::validateSettings)

    suspend fun updateSettings(request: SettingsPatchRequestDto): UserSettingsDto {
        require(request.easyMode != null || request.saveHistory != null || request.retentionDays != null)
        require(request.retentionDays == null || request.retentionDays in setOf(7, 30, 90))
        return api.updateSettings(request).requireResult(200, "SETTINGS200").also(::validateSettings)
    }

    suspend fun getUsage(): UsageDto {
        val result = api.getUsage().requireResult(200, "USAGE200")
        checked(result.plan == "BASIC" && result.limit != null && result.limit >= 0 &&
            result.used != null && result.used >= 0 && result.reserved != null && result.reserved >= 0 &&
            result.remaining != null && result.remaining >= 0 && !result.resetsAt.isNullOrBlank(),
            "Incomplete usage response")
        return result
    }

    private fun validateSettings(result: UserSettingsDto) {
        checked(result.easyMode != null && result.saveHistory != null &&
            result.retentionDays in setOf(7, 30, 90) && result.onboardingCompleted != null,
            "Incomplete settings response")
    }
}
