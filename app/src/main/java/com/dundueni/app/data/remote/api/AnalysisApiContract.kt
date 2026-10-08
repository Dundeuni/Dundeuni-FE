package com.dundueni.app.data.remote.api

/** BE 미확정 임시 계약. 실제 연결 전에 method/path/field를 함께 확인합니다. */
/**
 * 분석 요청의 HTTP 방식·상대 경로·이미지 필드명을 한곳에 모은다.
 * ApiService는 방식과 경로를, 입력 준비 단계는 이미지 필드명을 사용한다.
 * 경로는 RetrofitClient에 전달한 서버 기본 주소 뒤에 붙는다.
 * 요청을 만드는 쪽과 보내는 쪽이 같은 임시 계약을 사용하도록 연결한다.
 */
object AnalysisApiContract {
    const val METHOD = "POST"
    const val PATH = "analysis"

    // ImageMultipartFactory.createPart의 fieldName에 전달합니다.
    // 서비스는 전달받은 Part의 field명/filename/MIME를 변경하지 않습니다.
    const val IMAGE_FIELD = "image"
}
