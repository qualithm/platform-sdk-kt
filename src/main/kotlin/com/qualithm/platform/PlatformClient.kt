package com.qualithm.platform

import com.qualithm.platform.generated.infrastructure.ClientError
import com.qualithm.platform.generated.infrastructure.ClientException
import com.qualithm.platform.generated.infrastructure.ServerError
import com.qualithm.platform.generated.infrastructure.ServerException
import java.util.concurrent.ConcurrentHashMap
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * The Qualithm Platform API, authenticated with one API token.
 *
 * Each generated `…Api` class is one resource group from the spec. [api] binds one to this client,
 * and [call] runs calls so that every failure is a [PlatformError]:
 * ```kotlin
 * val platform = PlatformClient()
 * val page = platform.call { api(::DevicesApi).listDevices() }
 * ```
 *
 * Generated calls block the calling thread, so on Android make them off the main thread.
 *
 * @param token An API token (`qmt_…`). Defaults to `QUALITHM_API_TOKEN`.
 * @param baseUrl The API's base URL. Defaults to `QUALITHM_API_URL`, else [DEFAULT_BASE_URL].
 * @param environment Where the defaults are read from.
 * @param httpClient The OkHttp client to send requests with; this client adds the token to a copy
 *   of it, sharing its connection pool. Defaults to a new one.
 * @throws PlatformError.MissingToken when no token is given or set.
 */
public class PlatformClient(
    token: String? = null,
    baseUrl: String? = null,
    environment: Map<String, String> = System.getenv(),
    httpClient: OkHttpClient? = null,
) {
    /** The API's base URL, without a trailing `/`. */
    public val baseUrl: String

    /**
     * The OkHttp client every generated API class sends with; it adds the token to each request.
     */
    public val httpClient: OkHttpClient

    private val apis = ConcurrentHashMap<Any, Any>()

    init {
        fun set(name: String) = environment[name]?.takeIf { it.isNotEmpty() }
        val bearer = (token ?: set("QUALITHM_API_TOKEN"))?.takeIf { it.isNotEmpty() }
        if (bearer == null) throw PlatformError.MissingToken()
        this.baseUrl = (baseUrl ?: set("QUALITHM_API_URL") ?: DEFAULT_BASE_URL).trimEnd('/')
        val authorize = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder().header("Authorization", "Bearer $bearer").build()
            )
        }
        // First, so the caller's own interceptors see the request as sent.
        this.httpClient =
            (httpClient ?: OkHttpClient())
                .newBuilder()
                .apply { interceptors().add(0, authorize) }
                .build()
    }

    /**
     * One generated API class, bound to this client and created once per factory.
     *
     * @param factory A generated API class's constructor, such as `::DevicesApi`.
     * @return The instance for this client.
     */
    public fun <T : Any> api(factory: (basePath: String, client: Call.Factory) -> T): T {
        @Suppress("UNCHECKED_CAST")
        return apis.computeIfAbsent(factory) { factory(baseUrl, httpClient) } as T
    }

    /**
     * Runs generated calls and turns every failure into a [PlatformError].
     *
     * @param operation The calls to run, with this client as the receiver.
     * @return The operation's result.
     * @throws PlatformError for every failure.
     */
    public fun <T> call(operation: PlatformClient.() -> T): T =
        try {
            operation()
        } catch (error: PlatformError) {
            throw error
        } catch (error: ClientException) {
            throw PlatformError.fromResponse(
                error.statusCode,
                (error.response as? ClientError<*>)?.body as? String,
                error,
            )
        } catch (error: ServerException) {
            throw PlatformError.fromResponse(
                error.statusCode,
                (error.response as? ServerError<*>)?.body as? String,
                error,
            )
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            throw PlatformError.fromCause(error)
        }

    /** Constants shared by every client. */
    public companion object {
        /** The production API; `QUALITHM_API_URL` or `baseUrl` points the client elsewhere. */
        public const val DEFAULT_BASE_URL: String = "https://api.qualithm.com"
    }
}
