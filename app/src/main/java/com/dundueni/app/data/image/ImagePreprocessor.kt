package com.dundueni.app.data.image

import android.content.ContentResolver
import android.provider.OpenableColumns
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.model.PreprocessedImage

/**
 * ImageInput을 받아 전송에 필요한 정보가 담긴 PreprocessedImage로 정리한다.
 * 사진 URI는 ContentResolver를 통해 파일 제공자에게 정보를 조회한다.
 * ContentResolver는 URI가 가리키는 데이터에 접근하는 Android 통로다.
 * Bitmap은 그대로 넘기며 이미지 크기나 내용을 바꾸지 않는다.
 * 여기서 전처리는 입력 정보 정리이며 실제 AI 분석을 뜻하지 않는다.
 */
class ImagePreprocessor(private val contentResolver: ContentResolver) {
    fun preprocess(input: ImageInput): PreprocessedImage = when (input) {
        is ImageInput.PhotoPicker -> preprocess(input)
        is ImageInput.ScreenCapture -> preprocess(input)
    }

    fun preprocess(input: ImageInput.PhotoPicker): PreprocessedImage.PhotoPicker {
        // MIME 타입은 image/png처럼 파일의 데이터 형식을 나타낸다.
        val mimeType = contentResolver.getType(input.uri)
        var displayName: String? = null
        var sizeBytes: Long? = null

        // URI 제공자가 알려 주는 파일명과 크기를 조회하고, 없는 정보는 비워 둔다.
        contentResolver.query(
            input.uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex)
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        return PreprocessedImage.PhotoPicker(input.uri, mimeType, displayName, sizeBytes)
    }

    fun preprocess(input: ImageInput.ScreenCapture): PreprocessedImage.ScreenCapture =
        PreprocessedImage.ScreenCapture(input.bitmap)
}
