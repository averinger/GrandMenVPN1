package com.grandmenvpn.app.scanner

import com.grandmenvpn.app.parser.ParserRegistry
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
    private const val TCP_TIMEOUT_MS = 8000
    private const val SOCKS5_HOST = "127.0.0.1"
    private const val SOCKS5_PORT = 1080
    private const val IP_CHECK_HOST = "ipapi.co"
    private const val IP_CHECK_PORT = 80

    suspend fun scan(): List<FoundServer> = withContext(Dispatchers.Default) {
        val raw = downloadAll()
        val parsed = parseAll(raw)
        val deduped = dedup(parsed)
        val pinged = tcpPingAll(deduped)
        return@withContext pinged
    }

    private suspend fun downloadAll(): List<Pair<String, String>> {
        val sem = Semaphore(DOWNLOAD_CONCURRENCY)
        val out = mutableListOf<Pair<String, String>>()
        val lock = Mutex()
        coroutineScope {
            SourceList.urls.forEach { url ->
                async {
                    sem.withPermit {
                        val text = try { download(url) } catch (_: Exception) { "" }
                        lock.withLock { out += Pair(url, text) }
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

    private suspend fun verifyIpAll(list: List<FoundServer>): List<FoundServer> {
        val candidates = list.filter { it.pingMs > 0 }
        val sem = Semaphore(50)
        return coroutineScope {
            candidates.map { s ->
                async {
                    sem.withPermit {
                        val ok = verifyIpViaSocks5(s)
                        Pair(s, ok)
                    }
                }
            }.map { it.await() }
        }.filter { it.second }.map { it.first }
    }

    private suspend fun verifyIpViaSocks5(s: FoundServer): Boolean = withContext(Dispatchers.IO) {
        val directIp = fetchIpDirectly() ?: return@withContext false
        val proxyIp = fetchIpThroughLocalSocks5() ?: return@withContext false
        proxyIp.isNotEmpty() && proxyIp != directIp
    }

    private suspend fun fetchIpDirectly(): String? = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url("http://$IP_CHECK_HOST/ip").build())
                .execute().use { response -> response.body?.string()?.trim() }
        } catch (_: Exception) { null }
    }

    private suspend fun fetchIpThroughLocalSocks5(): String? = withContext(Dispatchers.IO) {
        Socket().use { socket ->
            try {
                socket.connect(InetSocketAddress(SOCKS5_HOST, SOCKS5_PORT), TCP_TIMEOUT_MS)
                socket.soTimeout = TCP_TIMEOUT_MS
                val out = socket.outputStream
                val inn = socket.inputStream
                out.write(byteArrayOf(0x05, 0x01, 0x00)); out.flush()
                if (inn.read() != 0x05 || inn.read() != 0x00) return@use null
                val addr = IP_CHECK_HOST.toByteArray(Charsets.UTF_8)
                out.write(byteArrayOf(0x05, 0x01, 0x00, 0x03))
                out.write(addr.size); out.write(addr)
                out.write((IP_CHECK_PORT shr 8).toInt()); out.write((IP_CHECK_PORT and 0xFF).toInt())
                out.flush()
                if (inn.read() != 0x05 || inn.read() != 0x00) return@use null
                inn.read()
                val atyp = inn.read()
                when (atyp) {
                    0x01 -> repeat(4) { inn.read() }
                    0x03 -> { val n = inn.read(); repeat(n) { inn.read() } }
                    0x04 -> repeat(16) { inn.read() }
                    else -> return@use null
                }
                repeat(2) { inn.read() }
                val req = "GET /ip HTTP/1.1\r\nHost: $IP_CHECK_HOST\r\nConnection: close\r\n\r\n"
                out.write(req.toByteArray(Charsets.UTF_8)); out.flush()
                val buf = StringBuilder()
                val chunk = ByteArray(1024)
                var len: Int
                while (inn.read(chunk).also { len = it } > 0) {
                    buf.append(String(chunk, 0, len, Charsets.UTF_8))
                }
                val body = buf.toString()
                val idx = body.indexOf("\r\n\r\n")
                if (idx < 0) return@use null
                val content = body.substring(idx + 4).trim()
                content.takeWhile { it.isDigit() || it == '.' }
            } catch (_: Exception) {
                null
            }
        }
    }
}