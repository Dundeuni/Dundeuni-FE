package com.dundueni.app.data.remote.api

import java.io.IOException

/** HTTP failure with the HTTP status and any structured BE error fields preserved. */
class AnalysisHttpException(
    val httpStatus: Int,
    val code: String?,
    val serverMessage: String?
) : IOException(serverMessage ?: "Analysis request failed with HTTP $httpStatus")

/** A successful HTTP response that does not satisfy the documented Analysis envelope. */
class AnalysisProtocolException(
    message: String,
    cause: Throwable? = null,
    val serverStatus: String? = null
) : IOException(message, cause)
