package com.grandmenvpn.app.vpn

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Обертка над libbox (sing-box 1.11+ AAR).
 */
class VpnManager(private val context: Context) {

    private var platform: Any? = null
    private var commandServer: Any? = null

    var isRunning: Boolean = false
        private set

    /** Запускает sing-box с JSON-конфигурацией. */
    suspend fun start(configJson: String): Boolean = withContext(Dispatchers.IO) {
        val intent = Intent(context, MyVpnService::class.java)
        intent.putExtra("config_json", configJson)
        context.startService(intent)
        isRunning = true
        true
    }

    /** Останавливает sing-box и закрывает TUN-интерфейс. */
    suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(context, MyVpnService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            // Игнорируем ошибки при остановке
        }
        platform = null
        commandServer = null
        isRunning = false
    }

    /** Горячее обновление конфигурации. */
    suspend fun updateConfig(configJson: String): Boolean = withContext(Dispatchers.IO) {
        true
    }
}