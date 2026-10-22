package com.grandmenvpn.app.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * Заглушка VPN-сервиса. Реальная реализация (libbox/sing-box) появится позже.
 */
class GrandMenVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.grandmenvpn.app.START"
        const val ACTION_STOP = "com.grandmenvpn.app.STOP"
    }
}
