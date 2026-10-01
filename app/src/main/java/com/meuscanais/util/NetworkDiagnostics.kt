package com.meuscanais.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.URL
import kotlin.system.measureTimeMillis

object NetworkDiagnostics {

    suspend fun measurePing(host: String): Long = withContext(Dispatchers.IO) {
        try {
            val address = host.replace("http://", "").replace("https://", "").split(":")[0].split("/")[0]
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(address).isReachable(3000)
            if (reachable) System.currentTimeMillis() - start else -1L
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun measureDownloadSpeed(host: String): Double = withContext(Dispatchers.IO) {
        try {
            // IPTV servers always have player_api.php. It might return a help page or metadata.
            val testUrl = URL("${host.removeSuffix("/")}/player_api.php")
            val connection = testUrl.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            val start = System.currentTimeMillis()
            var bytesRead = 0
            val buffer = ByteArray(16384) // Larger buffer for speed
            val input = connection.getInputStream()
            
            // Read for max 5 seconds to get a more accurate sample
            while (System.currentTimeMillis() - start < 5000) {
                val read = input.read(buffer)
                if (read == -1) break
                bytesRead += read
            }
            input.close()
            
            val end = System.currentTimeMillis()
            val durationSeconds = (end - start) / 1000.0
            if (durationSeconds > 0.5) { // Ensure at least half second of data
                val bitsRead = bytesRead * 8.0
                (bitsRead / durationSeconds) / 1_000_000.0 // Mbps
            } else {
                // If it finished too fast, the file was small.
                // Fallback to a common speedtest server if server file is tiny
                measureExternalSpeed()
            }
        } catch (e: Exception) {
            measureExternalSpeed()
        }
    }

    private suspend fun measureExternalSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            // Fallback to a fast external file if the IPTV server doesn't return enough data
            val testUrl = URL("https://link.testfile.org/15MB")
            val connection = testUrl.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val start = System.currentTimeMillis()
            var bytesRead = 0
            val buffer = ByteArray(16384)
            val input = connection.getInputStream()
            while (System.currentTimeMillis() - start < 3000) {
                val read = input.read(buffer)
                if (read == -1) break
                bytesRead += read
            }
            input.close()
            val durationSeconds = (System.currentTimeMillis() - start) / 1000.0
            if (durationSeconds > 0) (bytesRead * 8.0 / durationSeconds) / 1_000_000.0 else 0.0
        } catch (_: Exception) { 0.0 }
    }
}
