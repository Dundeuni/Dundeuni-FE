package com.dundueni.app.data.remote.network

import android.content.ContentResolver
import com.dundueni.app.data.image.ImageUploadPreprocessor
import com.dundueni.app.data.model.PreprocessedImage
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * IO 스레드에서 이미지 바이트 검증·변환 후 Multipart를 만든다.
 * 본문은 최종 10 MiB 이하의 바이트를 소유하므로 URI 권한/원본 Bitmap 수명에 의존하지 않는다.
 */
class ImageMultipartFactory(contentResolver: ContentResolver, cacheDirectory: File) {
    private val preprocessor = ImageUploadPreprocessor(contentResolver, cacheDirectory)

    fun createPart(image: PreprocessedImage, fieldName: String): MultipartBody.Part {
        require(fieldName.isNotBlank()) { "Multipart field name must not be blank" }
        val upload = preprocessor.prepare(image)
        return MultipartBody.Part.createFormData(
            fieldName, upload.filename, upload.bytes.toRequestBody(upload.mimeType.toMediaType())
        )
    }

    fun createRequestBody(image: PreprocessedImage): RequestBody {
        val upload = preprocessor.prepare(image)
        return upload.bytes.toRequestBody(upload.mimeType.toMediaType())
    }
}
