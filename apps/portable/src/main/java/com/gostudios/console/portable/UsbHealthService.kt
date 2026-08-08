package com.gostudios.console.portable

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gostudios.console.sdk.Protocol
import com.gostudios.console.sdk.UsbHealthLocal

/**
 * Foreground service that runs a USB health scan when a device attaches /
 * detaches and posts a summary notification.
 */
class UsbHealthService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()

        val devices = UsbHealthLocal.enumerate(this)
        val poor = devices.count { it.health == Protocol.USB_HEALTH_POOR || it.health == Protocol.USB_HEALTH_FAIR }

        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("GoConsoleOS USB Health")
            .setContentText("${devices.size} volume(s) · $poor need attention")
            .setOngoing(false)
            .build()
        startForeground(1, n)
        stopSelf()
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "GoConsole USB Health", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    companion object {
        private const val CHANNEL = "gousb"
    }
}