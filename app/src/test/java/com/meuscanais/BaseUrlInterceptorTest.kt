package com.meuscanais

import com.meuscanais.data.network.BaseUrlInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BaseUrlInterceptorTest {

    private lateinit var interceptor: BaseUrlInterceptor

    @Before
    fun setUp() {
        interceptor = BaseUrlInterceptor()
    }

    @Test
    fun `iptv request scheme host and port are rewritten to active base url`() {
        interceptor.updateBaseUrl("http://new-iptv-server.com:8080/")

        var requestedUrl = ""
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                requestedUrl = request.url.toString()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("OK".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val originalRequest = Request.Builder()
            .url("http://old-iptv-server.com/player_api.php?username=user&password=pass")
            .build()

        val response = client.newCall(originalRequest).execute()
        assertEquals(200, response.code)
        assertEquals("http://new-iptv-server.com:8080/player_api.php?username=user&password=pass", requestedUrl)
    }

    @Test
    fun `github api request host is not altered by base url interceptor`() {
        interceptor.updateBaseUrl("http://iptv-server.com/")

        var requestedHost = ""
        var requestedScheme = ""
        var requestedPath = ""

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requestedHost = request.url.host
                requestedScheme = request.url.scheme
                requestedPath = request.url.encodedPath
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .addInterceptor(interceptor)
            .build()

        val githubRequest = Request.Builder()
            .url("https://api.github.com/repos/Rafael020703/MeusCanais/releases/latest")
            .build()

        val response = client.newCall(githubRequest).execute()
        assertEquals(200, response.code)
        assertEquals("api.github.com", requestedHost)
        assertEquals("https", requestedScheme)
        assertEquals("/repos/Rafael020703/MeusCanais/releases/latest", requestedPath)
    }

    @Test
    fun `github cdn asset request host is not altered`() {
        interceptor.updateBaseUrl("http://iptv-server.com/")

        var requestedHost = ""
        var requestedScheme = ""

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requestedHost = request.url.host
                requestedScheme = request.url.scheme
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("APK_BYTES".toResponseBody("application/vnd.android.package-archive".toMediaType()))
                    .build()
            }
            .addInterceptor(interceptor)
            .build()

        val cdnRequest = Request.Builder()
            .url("https://objects.githubusercontent.com/release-assets/MeusCanais-v1.0.apk")
            .build()

        val response = client.newCall(cdnRequest).execute()
        assertEquals(200, response.code)
        assertEquals("objects.githubusercontent.com", requestedHost)
        assertEquals("https", requestedScheme)
    }

    @Test
    fun `spoofed domain evilgithub_com is treated as iptv request and rewritten`() {
        interceptor.updateBaseUrl("http://active-iptv-server.com/")

        var requestedUrl = ""
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                requestedUrl = request.url.toString()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("OK".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val spoofedRequest = Request.Builder()
            .url("http://evilgithub.com/api/test")
            .build()

        val response = client.newCall(spoofedRequest).execute()
        assertEquals(200, response.code)
        assertEquals("http://active-iptv-server.com/api/test", requestedUrl)
    }
}
