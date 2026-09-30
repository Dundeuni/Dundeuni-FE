package com.dundueni.app.data.model

import android.net.Uri



// photo picker 로 긁어온 이미지 객체

data class SelectedImage(
    val uri: Uri,
    val mimeType: String?,
    // val fileName: String?,
    // val sizeBytes: Long?
    // 파일명이랑 사이즈는 필요하면 추후에 추가 가능
)

