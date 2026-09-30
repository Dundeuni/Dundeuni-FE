package com.dundueni.app.test

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PhotoPickerTestScreen() {

    val context = LocalContext.current

    var selectedImageUri by remember {
        mutableStateOf<String?>(null)
    }

    var imageReadResult by remember {
        mutableStateOf("이미지 읽기 전")
    }

    var imageMimeType by remember {
        mutableStateOf("MIME 타입 확인 전")
    }

    var imageFileName by remember {
        mutableStateOf("파일명 확인 전")
    }

    var imageFileSize by remember {
        mutableStateOf("파일 크기 확인 전")
    }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = PickVisualMedia()
        ) { uri ->

            if (uri != null) {

                // 1. URI
                selectedImageUri = uri.toString()

                // 2. MIME Type
                imageMimeType =
                    context.contentResolver.getType(uri)
                        ?: "MIME 타입 알 수 없음"

                // 3. 파일명 / 파일 크기
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        OpenableColumns.DISPLAY_NAME,
                        OpenableColumns.SIZE
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->

                    if (cursor.moveToFirst()) {

                        val nameIndex =
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                        val sizeIndex =
                            cursor.getColumnIndex(OpenableColumns.SIZE)

                        if (nameIndex >= 0) {
                            imageFileName = cursor.getString(nameIndex)
                        }

                        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {

                            val sizeBytes = cursor.getLong(sizeIndex)

                            imageFileSize =
                                "${sizeBytes} bytes (${sizeBytes / 1024.0 / 1024.0} MB)"
                        }
                    }
                }

                // 4. 실제 이미지 데이터 읽기
                try {
                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { inputStream ->

                            val firstByte = inputStream.read()

                            imageReadResult =
                                if (firstByte != -1) {
                                    "이미지 데이터 읽기 성공"
                                } else {
                                    "이미지 데이터가 비어 있음"
                                }
                        } ?: run {
                        imageReadResult = "InputStream 열기 실패"
                    }

                } catch (e: Exception) {
                    imageReadResult = "이미지 읽기 실패: ${e.message}"
                }

            } else {

                selectedImageUri = null
                imageReadResult = "사진 선택 취소"
                imageMimeType = "MIME 타입 확인 전"
                imageFileName = "파일명 확인 전"
                imageFileSize = "파일 크기 확인 전"
            }
        }

    Column(
        modifier = Modifier.padding(24.dp)
    ) {

        Button(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(
                        PickVisualMedia.ImageOnly
                    )
                )
            }
        ) {
            Text("사진 선택")
        }

        Text(text = selectedImageUri ?: "선택된 사진 없음")
        Text(text = imageReadResult)
        Text(text = "MIME: $imageMimeType")
        Text(text = "파일명: $imageFileName")
        Text(text = "파일 크기: $imageFileSize")
    }
}