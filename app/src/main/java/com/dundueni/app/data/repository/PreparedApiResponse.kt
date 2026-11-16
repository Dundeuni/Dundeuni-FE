package com.dundueni.app.data.repository

import com.dundueni.app.data.remote.dto.PreparedApiEnvelope
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/** Preserve the HTTP and BE error codes for future callers without changing existing UI errors. */
class PreparedApiHttpException(
    val httpStatus: Int,
    val apiCode: String?,
    val serverMessage: String?,
) : IOException(serverMessage ?: "Request failed with HTTP $httpStatus")

class PreparedApiProtocolException(message: String) : IOException(message)

internal fun checked(condition: Boolean, message: String) {
    if (!condition) throw PreparedApiProtocolException(message)
}

internal fun <T> Response<PreparedApiEnvelope<T>>.requireResult(status: Int, code: String): T {
    if (code() != status) throw failure()
    val envelope = body()
    checked(envelope?.isSuccess == true && envelope.code == code && envelope.result != null,
        "Unexpected $code response")
    return envelope!!.result!!
}

internal fun <T> Response<*>.requireData(status: Int, success: Boolean?, data: T?): T {
    if (code() != status) throw failure()
    checked(success == true && data != null, "Unexpected success response")
    return data!!
}

internal fun Response<*>.failure(): PreparedApiHttpException {
    val error = try {
        errorBody()?.use { Gson().fromJson(it.charStream(), PreparedApiEnvelope::class.java) }
    } catch (_: RuntimeException) { null }
    return PreparedApiHttpException(code(), error?.code, error?.message)
}
