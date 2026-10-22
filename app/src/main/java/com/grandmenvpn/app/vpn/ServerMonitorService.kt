package com.grandmenvpn.app.vpn

import com.grandmenvpn.app.scanner.FoundServer
import com.grandmenvpn.app.scanner.ServerScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Мониторинг активного VPN-сервера.
 * Проверяет пинг каждые 30 секунд и при обрыве автоматически переключается
 * на следующий рабочий сервер из результата сканирования.
 */
class ServerMonitorService(
    private val vpnManager: VpnManager,
    private val scanner: ServerScanner = ServerScanner,
    private val checkIntervalMs: Long = 30_000
) {
    private var job: Job? = null
    private var currentServer: FoundServer? = null
    var isMonitoring: Boolean = false
        private set

    fun start(server: FoundServer) {
        stop()
        currentServer = server
        val scope = CoroutineScope(Dispatchers.Default)
        job = scope.launch {
            while (isActive) {
                delay(checkIntervalMs)
                val ping = tcpPing(server.host, server.port)
                if (ping < 0) {
                    switchToNext()
                }
            }
        }
        isMonitoring = true
    }

    fun stop() {
        job?.cancel()
        job = null
        currentServer = null
        isMonitoring = false
    }

    private suspend fun switchToNext() {
        withContext(Dispatchers.Default) {
            val found = scanner.scan().filter { s ->
                s.host != currentServer?.host || s.port != currentServer?.port
            }
            if (found.isNotEmpty()) {
                val next = found.first()
                currentServer = next
                vpnManager.updateConfig(next.outboundJson)
            }
        }
    }

    private suspend fun tcpPing(host: String, port: Int): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        Socket().use { socket ->
            try {
                socket.connect(InetSocketAddress(host, port), 4000)
                System.currentTimeMillis() - start
            } catch (_: Exception) {
                -1
            }
        }
    }
}
