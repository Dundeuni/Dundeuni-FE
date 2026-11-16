package com.dundueni.app.feature.analysis

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.dundueni.app.data.image.ImageUploadPreprocessor
import java.io.File
import com.dundueni.app.data.model.PreprocessedImage
import com.dundueni.app.data.image.ImagePreprocessor
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.remote.api.AnalysisApiContract
import com.dundueni.app.data.remote.network.ImageMultipartFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okio.Buffer
import java.io.ByteArrayInputStream

data class PreparedPhotoUpload(val part: MultipartBody.Part, val preview: Bitmap)

/** 두 입력을 동일한 전처리/Multipart 경로로 연결합니다. Bitmap 소유권은 호출자에게 있습니다. */
/**
 * 사진 URI·캡처 Bitmap·저장된 PNG를 분석 요청에 사용할 이미지로 준비한다.
 * 입력 출처가 달라도 Repository에는 같은 Multipart Part를 넘길 수 있게 한다.
 * 파일 정보 확인과 본문 생성은 전처리기와 Multipart Factory에 맡긴다.
 * 화면이 멈추지 않도록 준비 작업은 파일 작업용 IO 영역에서 수행한다.
 * 준비된 Part는 ViewModel이 받아 Repository에 전달한다.
 */
class AnalysisInputProcessor(contentResolver: ContentResolver, cacheDirectory: File) {
    private val preprocessor = ImagePreprocessor(contentResolver)
    private val multipartFactory = ImageMultipartFactory(contentResolver, cacheDirectory)

    suspend fun prepare(input: ImageInput): MultipartBody.Part = withContext(Dispatchers.IO) {
        multipartFactory.createPart(preprocessor.preprocess(input), AnalysisApiContract.IMAGE_FIELD)
    }

    /** 저장된 캡처 PNG도 같은 크기 검증/변환 정책을 적용합니다. */
    suspend fun prepareCaptureFile(file: File): MultipartBody.Part = withContext(Dispatchers.IO) {
        // 10 MiB 이하면 원본 PNG를 유지하고, 초과하면 JPEG 90/85를 시도한다.
        check(file.isFile) { "Capture file is missing" }
        multipartFactory.createPart(
            PreprocessedImage.PhotoPicker(Uri.fromFile(file), "image/png", "capture.png", file.length()),
            AnalysisApiContract.IMAGE_FIELD
        )
    }

    suspend fun preparePhotoUpload(input: ImageInput): PreparedPhotoUpload = withContext(Dispatchers.IO) {
        val part = multipartFactory.createPart(preprocessor.preprocess(input), "file")
        PreparedPhotoUpload(part, preview(part))
    }

    suspend fun prepareCaptureUpload(file: File): PreparedPhotoUpload = withContext(Dispatchers.IO) {
        val part = prepareCaptureFile(file)
        PreparedPhotoUpload(part, preview(part))
    }

    private fun preview(part: MultipartBody.Part): Bitmap {
        val buffer = Buffer()
        checkNotNull(part.body).writeTo(buffer)
        val bytes = buffer.readByteArray()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Unable to display image")
        val sample = BitmapFactory.Options().apply {
            inSampleSize = generateSequence(1) { it * 2 }
                .first { bounds.outWidth / it <= 1200 && bounds.outHeight / it <= 1200 }
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sample)
            ?: error("Unable to display image")
        val orientation = ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = ImageUploadPreprocessor.orientationMatrix(orientation)
        if (matrix.isIdentity) return decoded
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        decoded.recycle()
        return oriented
    }
}
