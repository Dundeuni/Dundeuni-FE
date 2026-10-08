package com.dundueni.app.data.model

import android.graphics.Bitmap
import android.net.Uri

/**
 * Multipart 본문을 만들기 전에 이미지와 전송 정보를 모아 두는 모델이다.
 * URI 입력에는 MIME 타입·파일명·크기를 함께 담고, 모르는 정보는 null로 둔다.
 * Bitmap 입력은 이미지 자체를 보관해 다음 단계에서 PNG로 인코딩하게 한다.
 * PhotoPicker 형태는 선택한 사진뿐 아니라 저장된 캡처 PNG에도 사용된다.
 * 입력 출처보다 다음 단계에서 이미지를 어떻게 읽을지가 구분 기준이다.
 */
sealed class PreprocessedImage {
    data class PhotoPicker(
        val uri: Uri,
        val mimeType: String?,
        val displayName: String?,
        val sizeBytes: Long?
    ) : PreprocessedImage()

    // 원본 Bitmap을 공유하며, 수명과 정리는 기존 호출자가 관리한다.
    data class ScreenCapture(
        val bitmap: Bitmap
    ) : PreprocessedImage()
}
