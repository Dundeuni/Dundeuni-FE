package com.dundueni.app.feature.photopicker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.image.ImagePreprocessor
import com.dundueni.app.feature.analysis.AnalysisActivity

/**
 * 시스템 사진 선택기를 열고 선택 결과를 분석 화면에 연결하는 Compose 화면이다.
 * 선택 결과는 이미지 전체 대신 사진을 가리키는 Android 주소인 URI로 받는다.
 * 분석 화면은 이 주소로 사진을 읽으므로 화면 전환 때 큰 이미지 데이터를 넘기지 않는다.
 * 메뉴에서 온 자동 실행 요청과 화면 버튼의 수동 실행을 같은 선택기로 처리한다.
 * 선택하면 분석 화면을 열고, 취소하면 이 화면의 선택 상태를 초기화한다.
 * 사진 정보와 읽기 결과를 보여주는 기존 확인용 UI도 함께 남아 있다.
 */
@Composable
fun PhotoPickerTestScreen(openPickerOnLaunch: Boolean = false) {

    val context = LocalContext.current
    val imagePreprocessor = remember(context) {
        ImagePreprocessor(context.contentResolver)
    }

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

    // 시스템 선택기의 결과를 받을 통로를 등록하고, 선택한 사진의 URI를 받는다.
    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = PickVisualMedia()
        ) { uri ->

            if (uri != null) {

                // 선택한 URI와 읽기 권한을 담은 Intent로 분석 화면을 연다.
                context.startActivity(AnalysisActivity.forPhoto(context, uri))

                // 선택한 사진의 URI를 ImageInput 에 넘김.
                val imageInput = ImageInput.PhotoPicker(uri)

                val preprocessedImage = imagePreprocessor.preprocess(imageInput)

                selectedImageUri = preprocessedImage.uri.toString()
                imageMimeType = preprocessedImage.mimeType ?: "MIME 타입 알 수 없음"
                imageFileName = preprocessedImage.displayName ?: "파일명 알 수 없음"
                imageFileSize = preprocessedImage.sizeBytes?.let { sizeBytes ->
                    "${sizeBytes} bytes (${sizeBytes / 1024.0 / 1024.0} MB)"
                } ?: "파일 크기 알 수 없음"

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

                // 선택 취소 시 분석 화면으로 이동하지 않고 이전 선택 정보도 지운다.
                selectedImageUri = null
                imageReadResult = "사진 선택 취소"
                imageMimeType = "MIME 타입 확인 전"
                imageFileName = "파일명 확인 전"
                imageFileSize = "파일 크기 확인 전"
            }
        }

    // 메뉴에서 전달된 요청만 한 번 실행한다. 회전/복원 시 Picker를 중복 실행하지 않는다.
    var pendingPickerLaunch by rememberSaveable {
        mutableStateOf(openPickerOnLaunch)
    }
    LaunchedEffect(pendingPickerLaunch) {
        if (pendingPickerLaunch) {
            // 실행 전에 요청을 소비해 화면 재구성이나 복원으로 자동 선택기가 다시 열리지 않게 한다.
            pendingPickerLaunch = false
            photoPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }

    Column(
        modifier = Modifier.padding(24.dp)
    ) {

        Button(
            onClick = {
                // 자동 실행 요청이 없어도 버튼을 눌러 이미지 한 장을 선택할 수 있다.
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
