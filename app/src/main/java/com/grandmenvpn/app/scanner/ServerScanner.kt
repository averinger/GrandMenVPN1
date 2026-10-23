package com.grandmenvpn.app.scanner

import com.grandmenvpn.app.parser.ParserRegistry
import com.grandmenvpn.app.utils.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.text.Charsets
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Duration

data class FoundServer(
    val protocol: String,
    val host: String,
    val port: Int,
    val pingMs: Long = -1,
    val source: String = "",
    val config: String = "",
    val outboundJson: String = ""
)

object ServerScanner {
    private val client = OkHttpClient.Builder()
        .connectTimeout(Duration.ofMillis(10_000))
        .readTimeout(Duration.ofMillis(10_000))
        .build()

    private const val DOWNLOAD_CONCURRENCY = 10
    private const val TCP_CONCURRENCY = 200
    private const val TCP_TIMEOUT_MS = 8000 // Увеличили до 8 секунд
    private const val SOCKS5_HOST = "127.0.0.1"
    private const val SOCKS5_PORT = 1080
    private const val IP_CHECK_HOST = "ipapi.co"
    private const val IP_CHECK_PORT = 80

    suspend fun scan(): List<FoundServer> = withContext(Dispatchers.Default) {
        DebugLogger.log("Scanner", "Начало сканирования серверов...")
        val raw = downloadAll()
        val parsed = parseAll(raw)
        DebugLogger.log("Scanner", "Спарсено серверов: ${parsed.size}")
        val deduped = dedup(parsed)
        DebugLogger.log("Scanner", "После дедупликации: ${deduped.size}")
        val pinged = tcpPingAll(deduped)
        
        // Отключили проверку IP через SOCKS5, чтобы не отсеивать сервера зря
        val result = pinged.filter { it.pingMs > 0 }
        DebugLogger.log("Scanner", "Рабочих серверов найдено: ${result.size}")
        result
    }

    private suspend fun downloadAll(): List<Pair<String, String>> {
        val sem = Semaphore(DOWNLOAD_CONCURRENCY)
        val out = mutableListOf<Pair<String, String>>()
        val lock = Mutex()
        coroutineScope {
            SourceList.urls.forEach { url ->
                async {
                    sem.withPermit {
                        val text = try { 
                            download(url) 
                        } catch (e: Exception) { 
                            DebugLogger.error("Scanner", "Ошибка скачивания $url", e)
                            "" 
                        }
                        if (text.isNotEmpty()) {
                            lock.withLock { out += Pair(url, text) }
                        }
                    }
                }
            }
        }
        return out
    }

    private suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    private suspend fun parseAll(raw: List<Pair<String, String>>): List<FoundServer> {
        val out = mutableListOf<FoundServer>()
        for ((source, text) in raw) {
            for (line in text.lines()) {
                val input = line.trim()
                if (input.isEmpty()) continue
                val json = ParserRegistry.parse(input) ?: continue
                val (proto, host, port) = extract(input)
                if (host.isEmpty() || port <= 0) continue
                out += FoundServer(
                    protocol = proto,
                    host = host,
                    port = port,
                    source = source,
                    config = input,
                    outboundJson = json
                )
            }
        }
        return out
    }

    private fun extract(input: String): Triple<String, String, Int> {
        val low = input.lowercase()
        val proto = when {
            low.startsWith("vless://") -> "VLESS"
            low.startsWith("vmess://") -> "VMess"
            low.startsWith("trojan://") -> "Trojan"
            low.startsWith("ss://") -> "Shadowsocks"
            low.startsWith("hysteria2://") || low.startsWith("hy2://") -> "Hysteria2"
            low.startsWith("tuic://") -> "TUIC"
            low.contains("[interface]") || low.contains("[peer]") -> "AmneziaWG"
            else -> "Unknown"
        }
        var host = ""
        var port = 0
        try {
            val uri = java.net.URI(input)
            host = uri.host ?: ""
            port = uri.port
            if (port <= 0) {
                port = when { proto == "Hysteria2" || proto == "TUIC" -> 443 else -> 0 }
            }
        } catch (_: Exception) { }
        return Triple(proto, host, port)
    }

    private suspend fun dedup(list: List<FoundServer>): List<FoundServer> =
        list.distinctBy { "${it.host}:${it.port}" }

    private suspend fun tcpPing(host: String, port: Int): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        Socket().use { socket ->
            try {
                socket.connect(InetSocketAddress(host, port), TCP_TIMEOUT_MS)
                System.currentTimeMillis() - start
            } catch (_: Exception) {
                -1
            }
        }
    }

    private suspend fun tcpPingAll(list: List<FoundServer>): List<FoundServer> {
        val sem = Semaphore(TCP_CONCURRENCY)
        return coroutineScope {
            list.map { s ->
                async {
                    sem.withPermit { s.copy(pingMs = tcpPing(s.host, s.port)) }
                }
            }.map { it.await() }
        }
    }
    
    suspend fun pingSingleServer(host: String, port: Int): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
                System.currentTimeMillis() - start
            }
        } catch (e: Exception) {
            -1 // Если не удалось пропинговать
        }
    }

    // Функция verifyIpAll удалена, так как мы теперь показываем все сервера, ответившие на пинг
}