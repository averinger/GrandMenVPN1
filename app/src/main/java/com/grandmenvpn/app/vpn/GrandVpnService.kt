package com.grandmenvpn.app.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class GrandVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnManager = VpnManager(this)

    override fun onCreate() {
        super.onCreate()
        VpnNotificationManager.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = VpnNotificationManager.buildNotification(this)
        startForeground(VpnNotificationManager.NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }

    /**
     * Настройка TUN-интерфейса:
     *   MTU 1400, адрес 172.19.0.1/30, маршруты 0.0.0.0/0 и ::/0.
     */
    fun buildTunBuilder(): VpnService.Builder = Builder()
        .addAddress("172.19.0.1", 30)
        .addRoute("0.0.0.0", 0)
        .addRoute("::", 0)
        .setMtu(1400)
}
