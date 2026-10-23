package com.grandmenvpn.app.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class MyVpnService : VpnService() {

    private var tunFd: ParcelFileDescriptor? = null

    // JNI-заглушка для запуска sing-box
    external fun startSingBox(tunFd: Int, configJson: String): Boolean

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val configJson = intent?.getStringExtra("config_json") ?: return START_NOT_STICKY

        if (tunFd != null) {
            Log.d("MyVpnService", "VPN уже запущен")
            return START_STICKY
        }

        try {
            val builder = Builder()
            builder.setSession("GrandMenVPN")
            builder.addAddress("10.0.0.2", 32)
            builder.addRoute("0.0.0.0", 0)
            builder.addDnsServer("1.1.1.1")
            
            // Устанавливаем соединение и получаем файловый дескриптор TUN-интерфейса
            tunFd = builder.establish()

            if (tunFd == null) {
                Log.e("MyVpnService", "Не удалось создать TUN-интерфейс")
                return START_NOT_STICKY
            }

            // Передаем дескриптор в sing-box
            try {
                startSingBox(tunFd!!.fd, configJson)
            } catch (e: Throwable) {
                Log.e("MyVpnService", "Ошибка запуска sing-box (библиотека не подключена): ${e.message}")
                
                // ВАЖНО: Закрываем TUN и останавливаем службу, чтобы не было "черной дыры"!
                tunFd?.close()
                tunFd = null
                stopSelf()
                return START_NOT_STICKY
            }

            return START_STICKY

        } catch (e: Exception) {
            Log.e("MyVpnService", "Ошибка VPN: ${e.message}")
            return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tunFd?.close()
        tunFd = null
        Log.d("MyVpnService", "VPN остановлен")
    }
}