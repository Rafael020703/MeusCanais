package com.meuscanais.data.network

import com.meuscanais.data.api.XtreamService
import nl.adaptivity.xmlutil.serialization.XML
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber

object RetrofitClient {
    private const val USER_AGENT = "IPTVSmarters"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val baseUrlInterceptor = BaseUrlInterceptor()

    val xml = XML {
        // XML configuration for version 1.0.2.1
    }

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor { message ->
            // Sanitize logs: Redact password and sensitive query params
            val redactedMessage = message
                .replace(Regex("password=[^&]*"), "password=[REDACTED]")
                .replace(Regex("username=[^&]*"), "username=[REDACTED]")
                .replace(Regex("token=[^&]*"), "token=[REDACTED]")
            Timber.tag("OkHttp").d(redactedMessage)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val playerOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun updateBaseUrl(newUrl: String) {
        var url = newUrl.trim()
        if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        Timber.d("Atualizando base URL para: %s", url)
        baseUrlInterceptor.updateBaseUrl(url)
    }

    fun createJsonService(baseUrl: String): XtreamService {
        var url = baseUrl.trim()
        if (url.isBlank()) {
            url = "http://localhost/"
        } else {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
        }
        if (!url.endsWith("/")) url += "/"
        
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(XtreamService::class.java)
    }

    fun createXmlService(baseUrl: String): EpgService {
        var url = baseUrl.trim()
        if (url.isBlank()) {
            url = "http://localhost/"
        } else {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
        }
        if (!url.endsWith("/")) url += "/"

        val xmlContentType = "application/xml".toMediaType()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(xml.asConverterFactory(xmlContentType))
            .build()
            .create(EpgService::class.java)
    }
}

class BaseUrlInterceptor : Interceptor {
    @Volatile
    private var baseUrl: String? = null

    fun updateBaseUrl(newUrl: String) {
        var url = newUrl.trim()
        if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        if (url.isNotEmpty() && !url.endsWith("/")) {
            url += "/"
        }
        this.baseUrl = url
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val currentBaseUrl = baseUrl

        // Do not alter scheme/host/port for external third-party services (GitHub API, GitHub CDN)
        if (isExternalHost(originalRequest.url.host)) {
            return chain.proceed(originalRequest)
        }

        val request = if (!currentBaseUrl.isNullOrBlank()) {
            val newUrl = currentBaseUrl.toHttpUrlOrNull()
            if (newUrl != null) {
                val updatedUrl = originalRequest.url.newBuilder()
                    .scheme(newUrl.scheme)
                    .host(newUrl.host)
                    .port(newUrl.port)
                    .build()
                originalRequest.newBuilder()
                    .url(updatedUrl)
                    .build()
            } else originalRequest
        } else originalRequest

        return chain.proceed(request)
    }

    private fun isExternalHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "api.github.com" ||
               h == "github.com" ||
               h == "githubusercontent.com" ||
               h.endsWith(".github.com") ||
               h.endsWith(".githubusercontent.com")
    }
}

interface EpgService {
    @retrofit2.http.GET
    suspend fun getEpg(@retrofit2.http.Url url: String): okhttp3.ResponseBody
}
