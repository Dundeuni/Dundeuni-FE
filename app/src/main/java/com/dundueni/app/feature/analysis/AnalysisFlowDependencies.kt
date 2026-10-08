package com.dundueni.app.feature.analysis

import com.dundueni.app.data.remote.api.FakeAnalysisApiService
import com.dundueni.app.data.repository.AnalysisRepository
import com.dundueni.app.data.repository.AnalysisResultRepository

/** 실제 API 연결 시 이 조립 지점에서만 Fake를 RetrofitClient.analysisApiService로 교체합니다. */
/**
 * 검사 화면에서 사용할 Repository와 API 구현을 조립하는 지점이다.
 * 현재는 서버 없이 흐름을 확인하도록 FakeAnalysisApiService를 연결한다.
 * Activity는 반환된 계약만 받아 ViewModel에 전달한다.
 * API 구현 선택을 한곳에 모아 화면과 상태 코드가 통신 방식에 얽히지 않게 한다.
 */
object AnalysisFlowDependencies {
    fun createRepository(): AnalysisResultRepository = AnalysisRepository(FakeAnalysisApiService())
}
