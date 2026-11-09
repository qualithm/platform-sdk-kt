package com.qualithm.platform

import com.qualithm.platform.generated.infrastructure.Serializer
import java.io.IOException

/**
 * Every failure a platform call raises: an error response from the API, a request that never got
 * one, or a client with no API token. Branch on [code], never on [message].
 *
 * @property message What failed, lowercase.
 */
public sealed class PlatformError(override val message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    /**
     * The envelope's stable code. A code this version doesn't know yet, or a failure with no
     * response, reads as [PlatformErrorCode.UNKNOWN]; a missing token reads as
     * [PlatformErrorCode.UNAUTHORIZED].
     */
    public abstract val code: PlatformErrorCode

    /** The HTTP status, or `0` when no response arrived. */
    public abstract val status: Int

    /**
     * The API answered with an error response.
     *
     * @property status The HTTP status.
     * @property code The envelope's stable code.
     */
    public class Api(
        override val status: Int,
        override val code: PlatformErrorCode,
        message: String,
        cause: Throwable? = null,
    ) : PlatformError(message, cause)

    /** The request never got a usable response. Its [status] is `0`. */
    public class Transport(message: String, cause: Throwable? = null) :
        PlatformError(message, cause) {
        override val code: PlatformErrorCode = PlatformErrorCode.UNKNOWN
        override val status: Int = 0
    }

    /** No API token was passed or set in `QUALITHM_API_TOKEN`. */
    public class MissingToken :
        PlatformError("api token missing: pass token or set QUALITHM_API_TOKEN") {
        override val code: PlatformErrorCode = PlatformErrorCode.UNAUTHORIZED
        override val status: Int = 0
    }

    internal companion object {
        private val envelopeAdapter = Serializer.moshi.adapter(Any::class.java)

        /** Builds the error for a non-2xx response, reading the `{ message, code }` envelope. */
        fun fromResponse(status: Int, body: String?, cause: Throwable): Api {
            val envelope =
                try {
                    body?.let { envelopeAdapter.fromJson(it) } as? Map<*, *>
                } catch (_: IOException) {
                    null
                }
            val code = PlatformErrorCode.fromValue(envelope?.get("code") as? String)
            val message =
                (envelope?.get("message") as? String)?.takeIf { it.isNotEmpty() }
                    ?: "request failed with status $status"
            return Api(status, code, message, cause)
        }

        /** Builds the error for a request that failed without an error response. */
        fun fromCause(cause: Throwable): Transport {
            val reason = cause.message?.takeIf { it.isNotEmpty() } ?: cause.javaClass.simpleName
            return Transport("request failed: ${reason.lowercase()}", cause)
        }
    }
}
