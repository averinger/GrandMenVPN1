package com.grandmenvpn.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.grandmenvpn.app.R

object VpnNotificationManager {
    private const val CHANNEL_ID = "grandmenvpn_vpn"
    private const val CHANNEL_NAME = "GrandMenVPN"
    const val NOTIFICATION_ID = 1

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            channel.description = "Управление VPN-подключением"
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context): Notification {
        val stopIntent = Intent(context, GrandVpnService::class.java).apply {
            action = "com.grandmenvpn.app.vpn.STOP"
        }
        val stopPending = PendingIntent.getService(
            context, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("GrandMenVPN")
            .setContentText("VPN подключён")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher, "Отключить", stopPending)
            .setContentIntent(stopPending)
            .build()
    }
}
