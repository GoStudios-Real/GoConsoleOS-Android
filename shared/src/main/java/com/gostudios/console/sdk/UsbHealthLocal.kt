package com.gostudios.console.sdk

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.storage.StorageManager
import android.os.storage.VolumeInfo
import java.util.Locale

/**
 * Local USB / volume health for Android 13 (API 33) and Android 16 (API 36).
 *
 * On Android the kernel owns the disk; we report what the framework exposes:
 * storage volumes with their state and free space, plus UsbManager devices for
 * vendor/product labels. The Windows host {go.console.health / GoConsole Health}
 * still performs the deep SMART check.
 */
object UsbHealthLocal {

    fun enumerate(context: Context): List<UsbDeviceInfo> {
        val out = mutableListOf<UsbDeviceInfo>()

        val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        if (sm != null) {
            for (v in sm.storageVolumes) {
                val dir = v.directory ?: continue
                val total = runCatching { sm.getTotalBytes(dir) }.getOrDefault(0L)
                val free = runCatching { sm.getAvailableBytes(dir) }.getOrDefault(0L)
                val mounted = v.state == VolumeInfo.STATE_MOUNTED
                val label = v.description ?: runCatching { dir.name }.getOrDefault("Volume")
                var health = Protocol.USB_HEALTH_UNKNOWN
                var score = 0

                if (mounted) {
                    val freeFraction = if (total > 0) free.toDouble() / total.toDouble() else 1.0
                    health = when {
                        freeFraction <= 0.02 -> Protocol.USB_HEALTH_POOR
                        freeFraction <= 0.10 -> Protocol.USB_HEALTH_FAIR
                        else -> Protocol.USB_HEALTH_OK
                    }
                    score = when (health) {
                        Protocol.USB_HEALTH_OK -> 90
                        Protocol.USB_HEALTH_FAIR -> 60
                        else -> 20
                    }
                } else if (v.state == VolumeInfo.STATE_MOUNTED_READ_ONLY) {
                    health = Protocol.USB_HEALTH_FAIR
                    score = 55
                }

                out.add(
                    UsbDeviceInfo(
                        id = v.id,
                        label = label,
                        vendor = "",
                        product = label,
                        serial = "",
                        health = health,
                        healthScore = score,
                        totalBytes = total,
                        freeBytes = free,
                        interfaceType = if (v.isRemovable) "USB" else "Internal",
                        mounted = mounted,
                    )
                )
            }
        }

        val um = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (um != null) {
            for (dev in um.deviceList.values) {
                out.add(
                    UsbDeviceInfo(
                        id = "usb:${dev.deviceId}",
                        label = dev.productName ?: dev.deviceName ?: "USB Device",
                        vendor = String.format(Locale.US, "Vendor %04X", dev.vendorId),
                        product = String.format(Locale.US, "Product %04X", dev.productId),
                        serial = dev.serialNumber ?: "",
                        health = Protocol.USB_HEALTH_UNKNOWN,
                        healthScore = 0,
                        interfaceType = if (dev.deviceClass == UsbDevice.CLASS_MASS_STORAGE) "USB Storage" else "USB Interface",
                        mounted = false,
                    )
                )
            }
        }

        return out
    }
}