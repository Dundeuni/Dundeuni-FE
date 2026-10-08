package com.dundueni.app.feature.analysis

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import com.dundueni.app.data.model.PreprocessedImage
import com.dundueni.app.data.image.ImagePreprocessor
import com.dundueni.app.data.model.ImageInput
import com.dundueni.app.data.remote.api.AnalysisApiContract
import com.dundueni.app.data.remote.network.ImageMultipartFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

/** 두 입력을 동일한 전처리/Multipart 경로로 연결합니다. Bitmap 소유권은 호출자에게 있습니다. */
/**
 * 사진 URI·캡처 Bitmap·저장된 PNG를 분석 요청에 사용할 이미지로 준비한다.
 * 입력 출처가 달라도 Repository에는 같은 Multipart Part를 넘길 수 있게 한다.
 * 파일 정보 확인과 본문 생성은 전처리기와 Multipart Factory에 맡긴다.
 * 화면이 멈추지 않도록 준비 작업은 파일 작업용 IO 영역에서 수행한다.
 * 준비된 Part는 ViewModel이 받아 Repository에 전달한다.
 */
class AnalysisInputProcessor(contentResolver: ContentResolver) {
    private val preprocessor = ImagePreprocessor(contentResolver)
    private val multipartFactory = ImageMultipartFactory(contentResolver)

    suspend fun prepare(input: ImageInput): MultipartBody.Part = withContext(Dispatchers.IO) {
        multipartFactory.createPart(preprocessor.preprocess(input), AnalysisApiContract.IMAGE_FIELD)
    }

    /** Activity 경계를 넘긴 캡처 PNG는 재디코딩/재인코딩 없이 같은 Factory로 전달합니다. */
    suspend fun prepareCaptureFile(file: File): MultipartBody.Part = withContext(Dispatchers.IO) {
        // 이미 저장된 PNG의 주소와 정보를 사용해 불필요한 이미지 변환을 피한다.
        check(file.isFile) { "Capture file is missing" }
        multipartFactory.createPart(
            PreprocessedImage.PhotoPicker(Uri.fromFile(file), "image/png", "capture.png", file.length()),
            AnalysisApiContract.IMAGE_FIELD
        )
    }
}
