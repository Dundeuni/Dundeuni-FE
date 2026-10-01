package com.dundueni.app.data.model

import android.net.Uri
import android.graphics.Bitmap



// MediaProjection, PhotoPicker, Camera, 등등으로 가져온 이미지를 담는 공통 클래스
sealed class ImageInput {

    data class PhotoPicker(
        val uri: Uri
    ) : ImageInput()

    data class ScreenCapture(
        val bitmap: Bitmap
    ) : ImageInput()
}




