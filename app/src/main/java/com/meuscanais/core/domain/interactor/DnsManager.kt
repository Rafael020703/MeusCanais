package com.meuscanais.core.domain.interactor

import com.meuscanais.data.network.RetrofitClient
import com.meuscanais.di.ApiOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsManager @Inject constructor(
    @ApiOkHttpClient private val okHttpClient: OkHttpClient
) {
    companion object {
        private val LOCAL_DNS_LIST = listOf(
            "http://ded30.com",
            "http://ded34.com",
            "http://ded15.com",
            "http://now1.org",
            "http://ppmm1.cc",
            "https://u123.life"
        )
    }

    fun updateBaseUrl(url: String) {
        RetrofitClient.updateBaseUrl(url)
    }

    fun getDnsOptions(): List<String> = LOCAL_DNS_LIST

    suspend fun testDns(url: String): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val client = okHttpClient.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).head().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code in 200..499) {
                System.currentTimeMillis() - start
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun findBestDns(): String = withContext(Dispatchers.IO) {
        val options = getDnsOptions()
        val results = options.map { url ->
            async {
                url to testDns(url)
            }
        }.awaitAll()

        val best = results.filter { it.second != -1L }.minByOrNull { it.second }
        if (best != null) {
            best.first
        } else {
            options.firstOrNull() ?: "http://ded30.com" // Fallback
        }
    }

    fun getNextDns(currentUrl: String): String {
        val options = getDnsOptions()
        val currentIndex = options.indexOfFirst { currentUrl.contains(it.removePrefix("http://").removePrefix("https://")) }
        if (currentIndex == -1) return options.firstOrNull() ?: currentUrl
        val nextIndex = (currentIndex + 1) % options.size
        return options[nextIndex]
    }
}
