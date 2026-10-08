package com.dundueni.app.data.remote.network

import android.content.ContentResolver
import android.graphics.Bitmap
import com.dundueni.app.data.model.PreprocessedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source

/**
 * 공통 전처리 결과를 전송 본문으로 변환합니다. 실제 네트워크 요청은 하지 않습니다.
 * Bitmap 인코딩과 URI 읽기는 작업 스레드에서 수행해야 합니다.
 * URI 권한은 본문 쓰기가 끝날 때까지 유지해야 하며, 원본 Bitmap은 여기서 recycle하지 않습니다.
 */
/**
 * 정리된 이미지 입력을 HTTP 요청에 실을 RequestBody와 Multipart Part로 만든다.
 * RequestBody는 이미지 바이트를 제공하고, Part는 필드명과 파일명을 함께 담는다.
 * 완성된 Part 하나가 분석 서버에 보낼 이미지 한 개를 표현한다.
 * URI는 원본 파일을 읽고, Bitmap은 전송 가능한 PNG 바이트로 바꾼다.
 * 입력 준비와 실제 API 호출을 나눠 같은 본문을 Fake에서도 사용할 수 있게 한다.
 */
class ImageMultipartFactory(private val contentResolver: ContentResolver) {
    fun createPart(image: PreprocessedImage, fieldName: String): MultipartBody.Part {
        require(fieldName.isNotBlank()) { "Multipart field name must not be blank" }
        val filename = when (image) {
            is PreprocessedImage.PhotoPicker -> image.displayName ?: UNKNOWN_FILENAME
            is PreprocessedImage.ScreenCapture -> CAPTURE_FILENAME
        }
        return MultipartBody.Part.createFormData(fieldName, filename, createRequestBody(image))
    }

    fun createRequestBody(image: PreprocessedImage): RequestBody = when (image) {
        is PreprocessedImage.PhotoPicker -> {
            val mediaType = (image.mimeType ?: UNKNOWN_MIME_TYPE).toMediaType()
            object : RequestBody() {
                override fun contentType() = mediaType

                override fun contentLength(): Long =
                    image.sizeBytes?.takeIf { it >= 0 } ?: -1L

                // 본문을 실제로 쓰는 시점에 URI를 열어 이미지 전체를 메모리에 미리 올리지 않는다.
                override fun writeTo(sink: BufferedSink) {
                    val input = contentResolver.openInputStream(image.uri)
                        ?: throw IOException("Unable to open image input stream")
                    input.source().use { source -> sink.writeAll(source) }
                }
            }
        }
        is PreprocessedImage.ScreenCapture -> {
            require(!image.bitmap.isRecycled) { "Screen capture bitmap is recycled" }
            // 메모리의 Bitmap을 현재 전송 형식인 PNG 바이트로 만들어 요청 본문에 담는다.
            val bytes = ByteArrayOutputStream().use { output ->
                // PNG는 무손실이며 quality 인자를 무시합니다. 리사이즈하지 않습니다.
                if (!image.bitmap.compress(CAPTURE_FORMAT, 100, output)) {
                    throw IOException("Unable to encode screen capture")
                }
                output.toByteArray()
            }
            bytes.toRequestBody(CAPTURE_MIME_TYPE.toMediaType())
        }
    }

    private companion object {
        // BE 계약 확정 전의 fallback/캡처 형식입니다. 형식·MIME·확장자는 함께 변경합니다.
        const val UNKNOWN_MIME_TYPE = "application/octet-stream"
        const val UNKNOWN_FILENAME = "image"
        const val CAPTURE_MIME_TYPE = "image/png"
        const val CAPTURE_FILENAME = "capture.png"
        val CAPTURE_FORMAT = Bitmap.CompressFormat.PNG
    }
}
