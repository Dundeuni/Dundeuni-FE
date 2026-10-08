package com.dundueni.app.data.model

import android.net.Uri
import android.graphics.Bitmap



// MediaProjection, PhotoPicker, Camera, 등등으로 가져온 이미지를 담는 공통 클래스
/**
 * 수집한 이미지를 입력 방식별로 구분해 전처리기에 넘기는 모델이다.
 * 사진은 읽을 위치인 URI로, 메모리의 캡처 이미지는 Bitmap으로 담는다.
 * 호출자는 같은 ImageInput 타입을 넘기고 전처리기가 종류에 맞게 처리한다.
 * 현재 정의된 입력은 PhotoPicker와 ScreenCapture 두 가지다.
 * 서버 전송 형식이나 분석 결과는 이 모델에서 다루지 않는다.
 */
sealed class ImageInput {

    data class PhotoPicker(
        val uri: Uri
    ) : ImageInput()

    data class ScreenCapture(
        val bitmap: Bitmap
    ) : ImageInput()
}




