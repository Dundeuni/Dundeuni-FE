package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.api.AnalysisHttpException
import com.dundueni.app.data.remote.api.AnalysisProtocolException
import com.dundueni.app.data.remote.api.PhotoSelectionApiService
import com.dundueni.app.data.remote.api.PhotoSelectionRequest
import com.dundueni.app.data.remote.api.PhotoUploadResponse
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class PhotoSelectionRepositoryTest {
    @Test fun cancellationRequiresHttp200AndDocumentedSuccessCode() = runBlocking {
        repository(Response.success(200, body("""{"isSuccess":true,"code":"IMAGE_UPLOAD200","result":null}""")))
            .cancel("upload-id")

        try {
            repository(Response.success(200, body("""{"isSuccess":true,"code":"WRONG200","result":null}""")))
                .cancel("upload-id")
            fail("Wrong success code was accepted")
        } catch (_: AnalysisProtocolException) { }

        try {
            repository(Response.error(404, body("""{"isSuccess":false,"code":"IMAGE_UPLOAD404","message":"Missing"}""")))
                .cancel("upload-id")
            fail("HTTP failure was accepted")
        } catch (error: AnalysisHttpException) {
            assertEquals(404, error.httpStatus)
            assertEquals("IMAGE_UPLOAD404", error.code)
        }
    }

    private fun repository(cancelResponse: Response<ResponseBody>): PhotoSelectionRepository {
        val api = object : PhotoSelectionApiService {
            override suspend fun upload(file: MultipartBody.Part): Response<PhotoUploadResponse> =
                error("Unexpected upload")

            override suspend fun select(uploadId: String, request: PhotoSelectionRequest): Response<ResponseBody> =
                error("Unexpected selection")

            override suspend fun cancel(uploadId: String): Response<ResponseBody> = cancelResponse
        }
        return PhotoSelectionRepository(api, AnalysisResultRepository { error("Unexpected analysis") })
    }

    private fun body(json: String) = json.toResponseBody("application/json".toMediaType())
}
