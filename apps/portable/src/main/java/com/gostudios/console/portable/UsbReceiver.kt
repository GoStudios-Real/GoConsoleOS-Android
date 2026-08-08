package com.gostudios.console.portable

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Trigger a USB health scan whenever storage hardware is attached/detached.
 */
class UsbReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.isNullOrEmpty()) return
        val serviceIntent = Intent(context, UsbHealthService::class.java).putExtra("action", intent.action)
        context.startService(serviceIntent)
    }
}