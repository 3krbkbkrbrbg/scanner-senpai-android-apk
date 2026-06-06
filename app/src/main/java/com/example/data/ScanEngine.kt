package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.system.measureTimeMillis

data class ScanResult(
    val ip: String,
    val port: Int,
    val isClean: Boolean,
    val latency: Long = -1,
    val downloadSpeed: Double = 0.0 // in KB/s
)

class ScanEngine {

    companion object {
        const val DEFAULT_HTTP_SPEEDTEST_URL = "https://speed.cloudflare.com/__down?bytes=500000" // 500 KB test payload
    }

    /**
     * Measures latency to a target IP and port. We attempt to open a basic TCP socket.
     */
    suspend fun checkLatency(ip: String, port: Int, timeoutMs: Int = 1200): ScanResult = withContext(Dispatchers.IO) {
        val socket = Socket()
        val address = InetSocketAddress(ip, port)
        var latency: Long = -1
        var success = false
        
        try {
            val timeTaken = measureTimeMillis {
                socket.connect(address, timeoutMs)
            }
            if (socket.isConnected) {
                latency = timeTaken
                success = true
            }
        } catch (e: Exception) {
            // connection failed
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
        
        ScanResult(ip = ip, port = port, isClean = success, latency = latency)
    }

    /**
     * Performs a download speed test through a specific Cloudflare IP.
     * Overrides DNS resolution of speed.cloudflare.com to test specific IPs directly.
     */
    suspend fun checkDownloadSpeed(ip: String, payloadBytes: Int = 500000, timeoutMs: Int = 5000): Double = withContext(Dispatchers.IO) {
        var speedKbps = 0.0
        var connection: java.net.HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            // Modern Kotlin HTTP connection overriding hostname resolution to force the target IP
            val urlString = "https://$ip/__down?bytes=$payloadBytes"
            val url = URL(urlString)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.setRequestProperty("Host", "speed.cloudflare.com")
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            
            // Bypass strict domain verification since we're using direct Cloudflare IP addresses
            if (connection is javax.net.ssl.HttpsURLConnection) {
                connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { hostname, session ->
                    true // Custom bypass for testing direct Cloudflare CDN IPs
                }
            }

            var totalBytesRead = 0
            val buffer = ByteArray(4096)
            
            val startTime = System.currentTimeMillis()
            inputStream = connection.inputStream
            
            while (true) {
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    break // safety cutout
                }
                val read = inputStream.read(buffer)
                if (read == -1) break
                totalBytesRead += read
            }
            
            val endTime = System.currentTimeMillis()
            val durationMs = endTime - startTime
            
            if (durationMs > 10 && totalBytesRead > 0) {
                val durationSec = durationMs / 1000.0
                val kbRead = totalBytesRead / 1024.0
                speedKbps = kbRead / durationSec // KB/s
            }
        } catch (e: Exception) {
            // speedtest failed or stalled
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) { /* ignore */ }
            try {
                connection?.disconnect()
            } catch (e: Exception) { /* ignore */ }
        }
        speedKbps
    }

    /**
     * Scans list of Cloudflare IPs in a flow to update real-time progress.
     */
    fun scanIps(
        ips: List<String>,
        port: Int,
        timeoutMs: Int = 1200,
        runSpeedTest: Boolean = false
    ): Flow<ScanResult> = flow {
        for (ip in ips) {
            var result = checkLatency(ip, port, timeoutMs)
            if (result.isClean && runSpeedTest) {
                val speed = checkDownloadSpeed(ip)
                result = result.copy(downloadSpeed = speed)
            }
            emit(result)
        }
    }.flowOn(Dispatchers.IO)
}
