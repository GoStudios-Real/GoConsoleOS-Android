package com.gostudios.console.vr

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import com.gostudios.console.vr.controller.BluetoothController

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var controllerStatus: TextView
    private lateinit var deviceInfo: TextView
    private lateinit var startButton: Button
    private lateinit var scanButton: Button
    private lateinit var controller: BluetoothController

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF0D0D14.toInt())
        }

        // Title
        layout.addView(TextView(this).apply {
            text = "GoConsole VR"
            textSize = 32f
            setTextColor(0xFF0066FF.toInt())
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Put your phone in a VR headset and use a Bluetooth controller"
            textSize = 14f
            setTextColor(0xFF7A80A0.toInt())
            setPadding(0, 0, 0, 32)
        })

        // Device Info
        deviceInfo = TextView(this).apply {
            text = "Device: ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nSDK: ${Build.VERSION.SDK_INT}"
            textSize = 13f
            setTextColor(0xFF4A4A6A.toInt())
            setPadding(0, 0, 0, 24)
        }
        layout.addView(deviceInfo)

        // Controller Status
        layout.addView(TextView(this).apply {
            text = "CONTROLLER STATUS"
            textSize = 12f
            setTextColor(0xFF4A4A6A.toInt())
            setPadding(0, 0, 0, 8)
        })

        controllerStatus = TextView(this).apply {
            text = "Scanning..."
            textSize = 16f
            setTextColor(0xFFFFAA00.toInt())
            setPadding(0, 0, 0, 24)
        }
        layout.addView(controllerStatus)

        // Scan Button
        scanButton = Button(this).apply {
            text = "Scan for Controllers"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1E2D42.toInt())
            setPadding(24, 16, 24, 16)
            setOnClickListener { scanControllers() }
        }
        layout.addView(scanButton)

        // Status
        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 14f
            setTextColor(0xFF7A80A0.toInt())
            setPadding(0, 24, 0, 24)
        }
        layout.addView(statusText)

        // Start VR Button
        startButton = Button(this).apply {
            text = "START VR MODE"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF0066FF.toInt())
            setPadding(32, 20, 32, 20)
            setOnClickListener { startVR() }
        }
        layout.addView(startButton)

        // Games info
        layout.addView(TextView(this).apply {
            text = "\n5 VR Games:\n- VR Pong\n- VR Snake\n- VR Space Shooter\n- VR Jigsaw\n- VR Basketball\n\nControls:\n- Gyroscope = Head tracking\n- Left stick = Move\n- A = Select/Shoot\n- B = Back\n- D-pad = Navigate\n- Triggers = Action"
            textSize = 13f
            setTextColor(0xFF7A80A0.toInt())
            setPadding(0, 16, 0, 0)
        })

        scrollView.addView(layout)
        setContentView(scrollView)

        controller = BluetoothController(this)
        controller.setListener(object : BluetoothController.ControllerListener {
            override fun onControllerConnected(state: BluetoothController.ControllerState) {
                runOnUiThread {
                    controllerStatus.text = "Connected: ${state.name} (${state.type})"
                    controllerStatus.setTextColor(0xFF00CC66.toInt())
                    statusText.text = "Controller ready! Press START VR MODE"
                }
            }
            override fun onControllerDisconnected(state: BluetoothController.ControllerState) {
                runOnUiThread {
                    controllerStatus.text = "Disconnected: ${state.name}"
                    controllerStatus.setTextColor(0xFFFF4444.toInt())
                }
            }
            override fun onButtonPressed(state: BluetoothController.ControllerState, button: Int, pressed: Boolean) {}
            override fun onJoystickMoved(state: BluetoothController.ControllerState, axis: Int, x: Float, y: Float) {}
            override fun onTriggerMoved(state: BluetoothController.ControllerState, axis: Int, value: Float) {}
        })
    }

    @SuppressLint("MissingPermission")
    private fun scanControllers() {
        statusText.text = "Scanning for Bluetooth controllers..."
        controller.start()
        val btManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        adapter?.bondedDevices?.forEach { device ->
            if (isGamepad(device)) {
                controllerStatus.text = "Found: ${device.name}"
                controllerStatus.setTextColor(0xFF00CC66.toInt())
            }
        }
        if (controllerStatus.text == "Scanning...") {
            controllerStatus.text = "No controller found - pair in Android Settings"
            controllerStatus.setTextColor(0xFFFFAA00.toInt())
        }
    }

    private fun isGamepad(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: return false
        return name.contains("xbox") || name.contains("ps4") || name.contains("ps5") ||
               name.contains("dualshock") || name.contains("dualsense") ||
               name.contains("switch") || name.contains("controller") ||
               name.contains("gamepad") || name.contains("nokia")
    }

    private fun startVR() {
        startActivity(Intent(this, VRActivity::class.java))
    }

    override fun onResume() { super.onResume(); controller.start() }
    override fun onPause() { super.onPause(); controller.stop() }
}

