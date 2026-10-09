package com.dundueni.app.data.remote.api

/**
 * BE PR #4의 IMAGE Multipart 요청 계약이다.
 * ApiService는 방식과 경로를, 입력 준비 단계는 이미지 필드명을 사용한다.
 * 경로는 서버 루트 기준으로 고정하여 base URL의 경로와 중복되지 않게 한다.
 */
object AnalysisApiContract {
    const val METHOD = "POST"
    const val PATH = "/api/analysis"
    const val TYPE_FIELD = "type"
    const val IMAGE_TYPE = "IMAGE"

    // ImageMultipartFactory.createPart의 fieldName에 전달합니다.
    // 서비스는 전달받은 Part의 field명/filename/MIME를 변경하지 않습니다.
    const val IMAGE_FIELD = "file"
}
