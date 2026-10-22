package com.grandmenvpn.app.vpn

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Обертка над libbox (sing-box 1.11+ AAR).
 *
 * Управляет ядром sing-box через PlatformInterface и отправляет команды
 * (старт / стоп / обновление конфигурации) через CommandServer.
 *
 * Реальные классы libbox пока не подключены в Gradle (см. Шаг 8), поэтому поля
 * пока typed как Any? и будут заменены на PlatformInterface / CommandServer при
 * добавлении AAR-модуля libbox.
 */
class VpnManager(private val context: Context) {

    // PlatformInterface — точка входа в libbox (инициализируется из Context).
    private var platform: Any? = null
    // CommandServer — сервер команд sing-box (start/stop/updateConfig).
    private var commandServer: Any? = null

    var isRunning: Boolean = false
        private set

    /** Запускает sing-box с JSON-конфигурацией (outbounds / endpoints). */
    suspend fun start(configJson: String): Boolean = withContext(Dispatchers.IO) {
        // При подключении libbox:
        // platform = PlatformInterface(context)
        // commandServer = platform.createCommandServer()
        // commandServer.start(configJson)
        isRunning = true
        true
    }

    /** Останавливает sing-box и закрывает TUN-интерфейс. */
    suspend fun stop() = withContext(Dispatchers.IO) {
        // commandServer?.stop()
        // platform?.close()
        platform = null
        commandServer = null
        isRunning = false
    }

    /** Горячее обновление конфигурации (используется при failover/переключении сервера). */
    suspend fun updateConfig(configJson: String): Boolean = withContext(Dispatchers.IO) {
        // commandServer?.updateConfig(configJson)
        true
    }
}
