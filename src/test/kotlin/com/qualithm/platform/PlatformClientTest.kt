package com.qualithm.platform

import com.qualithm.platform.generated.apis.DevicesApi
import com.qualithm.platform.generated.apis.HealthApi
import com.qualithm.platform.generated.models.ApiErrorCode
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** An interceptor that records each request and answers it without a network. */
private class Stub(
    private val status: Int = 200,
    private val body: String = "{}",
    private val failure: IOException? = null,
) : Interceptor {
    val requests = mutableListOf<Request>()

    override fun intercept(chain: Interceptor.Chain): Response {
        requests += chain.request()
        failure?.let { throw it }
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("stub")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }
}

class PlatformClientTest {
    private val teamId = "tm_01JAYQ4T7Z9K3M5N8P2R6S0V1W"
    private val deviceId = "dev_01JAYQ4T7Z9K3M5N8P2R6S0V1W"

    private fun client(
        stub: Stub,
        token: String? = "qmt_test",
        baseUrl: String? = null,
        environment: Map<String, String> = emptyMap(),
    ) =
        PlatformClient(
            token = token,
            baseUrl = baseUrl,
            environment = environment,
            httpClient = OkHttpClient.Builder().addInterceptor(stub).build(),
        )

    @Test
    fun `sends the token as a bearer header to the production API by default`() {
        val stub = Stub(body = """{"data":{"current":1,"items":[],"last":1}}""")
        val platform = client(stub)

        val page = platform.call { api(::DevicesApi).listDevices(teamId, page = 2, limit = 10) }

        assertTrue(page.`data`.items.isEmpty())
        val request = stub.requests.single()
        assertEquals(
            "https://api.qualithm.com/v1/teams/$teamId/devices?page=2&limit=10",
            request.url.toString(),
        )
        assertEquals("Bearer qmt_test", request.header("Authorization"))
    }

    @Test
    fun `reads the token and base URL from the environment`() {
        val stub = Stub(body = """{"status":"ok"}""")
        val platform =
            client(
                stub,
                token = null,
                environment =
                    mapOf(
                        "QUALITHM_API_TOKEN" to "qmt_env",
                        "QUALITHM_API_URL" to "https://api.dev.qualithm.com/",
                    ),
            )

        platform.call { api(::HealthApi).getHealthz() }

        val request = stub.requests.single()
        assertEquals("https://api.dev.qualithm.com/healthz", request.url.toString())
        assertEquals("Bearer qmt_env", request.header("Authorization"))
        assertEquals("https://api.dev.qualithm.com", platform.baseUrl)
    }

    @Test
    fun `prefers arguments over the environment`() {
        val stub = Stub(body = """{"status":"ok"}""")
        val platform =
            client(
                stub,
                token = "qmt_arg",
                baseUrl = "http://localhost:3000",
                environment =
                    mapOf(
                        "QUALITHM_API_TOKEN" to "qmt_env",
                        "QUALITHM_API_URL" to "https://example.com",
                    ),
            )

        platform.call { api(::HealthApi).getHealthz() }

        val request = stub.requests.single()
        assertEquals("http://localhost:3000/healthz", request.url.toString())
        assertEquals("Bearer qmt_arg", request.header("Authorization"))
    }

    @Test
    fun `creates each API class once`() {
        val platform = client(Stub())

        assertSame(platform.api(::DevicesApi), platform.api(::DevicesApi))
    }

    @Test
    fun `refuses to start without a token`() {
        val error =
            assertThrows(PlatformError.MissingToken::class.java) {
                PlatformClient(environment = mapOf("QUALITHM_API_TOKEN" to ""))
            }
        assertEquals(ApiErrorCode.UNAUTHORIZED, error.code)
        assertEquals(0, error.status)
        assertEquals("api token missing: pass token or set QUALITHM_API_TOKEN", error.message)
        assertThrows(PlatformError.MissingToken::class.java) {
            PlatformClient(token = "", environment = emptyMap())
        }
    }

    private fun failure(stub: Stub): PlatformError {
        val platform = client(stub)
        return assertThrows(PlatformError::class.java) {
            platform.call { api(::DevicesApi).getDevice(teamId, deviceId) }
        }
    }

    @Test
    fun `carries the envelope's code, message and status`() {
        val error = failure(Stub(404, """{"message":"device not found","code":"not_found"}"""))

        assertTrue(error is PlatformError.Api)
        assertEquals(ApiErrorCode.NOT_FOUND, error.code)
        assertEquals(404, error.status)
        assertEquals("device not found", error.message)
    }

    @Test
    fun `reads the envelope of a server error`() {
        val error =
            failure(Stub(503, """{"message":"backend unavailable","code":"backend_unavailable"}"""))

        assertEquals(ApiErrorCode.BACKEND_UNAVAILABLE, error.code)
        assertEquals(503, error.status)
        assertEquals("backend unavailable", error.message)
    }

    @Test
    fun `decodes an unknown code to the fallback`() {
        val error =
            failure(
                Stub(422, """{"message":"device limit reached","code":"device_limit_reached"}""")
            )

        assertEquals(ApiErrorCode.UNKNOWN_DEFAULT_OPEN_API, error.code)
        assertEquals(422, error.status)
        assertEquals("device limit reached", error.message)
    }

    @Test
    fun `names the status when the body is not an envelope`() {
        val error = failure(Stub(502, "<html>bad gateway</html>"))

        assertTrue(error is PlatformError.Api)
        assertEquals(ApiErrorCode.UNKNOWN_DEFAULT_OPEN_API, error.code)
        assertEquals(502, error.status)
        assertEquals("request failed with status 502", error.message)
    }

    @Test
    fun `wraps a request that never got a response`() {
        val error = failure(Stub(failure = IOException("Connection Refused")))

        assertTrue(error is PlatformError.Transport)
        assertEquals(0, error.status)
        assertEquals(ApiErrorCode.UNKNOWN_DEFAULT_OPEN_API, error.code)
        assertEquals("request failed: connection refused", error.message)
        assertTrue(error.cause is IOException)
    }

    @Test
    fun `names the failure when it has no message`() {
        val error = failure(Stub(failure = IOException()))

        assertEquals("request failed: ioexception", error.message)
    }

    @Test
    fun `passes a PlatformError through unchanged`() {
        val platform = client(Stub())
        val thrown = PlatformError.MissingToken()

        assertSame(
            thrown,
            assertThrows(PlatformError::class.java) { platform.call { throw thrown } },
        )
    }
}
