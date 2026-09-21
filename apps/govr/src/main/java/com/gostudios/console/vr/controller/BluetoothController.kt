package com.gostudios.console.vr.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

class BluetoothController(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var listener: ControllerListener? = null
    private val connectedControllers = mutableMapOf<Int, ControllerState>()
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    interface ControllerListener {
        fun onControllerConnected(controller: ControllerState)
        fun onControllerDisconnected(controller: ControllerState)
        fun onButtonPressed(controller: ControllerState, button: Int, pressed: Boolean)
        fun onJoystickMoved(controller: ControllerState, axis: Int, x: Float, y: Float)
        fun onTriggerMoved(controller: ControllerState, axis: Int, value: Float)
    }

    data class ControllerState(
        val id: Int,
        val name: String,
        val type: ControllerType = ControllerType.GENERIC,
        var connected: Boolean = true,
        var leftStick: PointF = PointF(0f, 0f),
        var rightStick: PointF = PointF(0f, 0f),
        var leftTrigger: Float = 0f,
        var rightTrigger: Float = 0f,
        var buttons: MutableMap<Int, Boolean> = mutableMapOf()
    )

    enum class ControllerType {
        XBOX, PS4, PS5, NINTENDO_SWITCH, GENERIC, NOKIA
    }

    fun setListener(l: ControllerListener?) { listener = l }

    fun start() {
        scanBluetoothControllers()
    }

    fun stop() {
        connectedControllers.clear()
    }

    @SuppressLint("MissingPermission")
    private fun scanBluetoothControllers() {
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            val type = detectControllerType(device.name ?: "")
            if (isController(type)) {
                Log.d("GoVR", "Found paired controller: ${device.name} ($type)")
            }
        }
    }

    private fun detectControllerType(name: String): ControllerType {
        val lower = name.lowercase()
        return when {
            lower.contains("xbox") || lower.contains("xinput") || lower.contains("microsoft") -> ControllerType.XBOX
            lower.contains("dualshock") || lower.contains("ps4") || lower.contains("wireless controller") -> ControllerType.PS4
            lower.contains("dualsense") || lower.contains("ps5") -> ControllerType.PS5
            lower.contains("switch") || lower.contains("nintendo") || lower.contains("pro controller") -> ControllerType.NINTENDO_SWITCH
            lower.contains("nokia") || lower.contains("md-11") -> ControllerType.NOKIA
            else -> ControllerType.GENERIC
        }
    }

    private fun isController(type: ControllerType) = true

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val deviceId = event.deviceId
        val state = getOrCreateController(deviceId)

        // Left stick (AXIS_X, AXIS_Y)
        val lx = getAxisValue(event, MotionEvent.AXIS_X)
        val ly = getAxisValue(event, MotionEvent.AXIS_Y)
        if (kotlin.math.abs(lx) > DEAD_ZONE || kotlin.math.abs(ly) > DEAD_ZONE) {
            state.leftStick = PointF(lx, ly)
            listener?.onJoystickMoved(state, 0, lx, ly)
        }

        // Right stick (AXIS_Z, AXIS_RZ)
        val rx = getAxisValue(event, MotionEvent.AXIS_Z)
        val ry = getAxisValue(event, MotionEvent.AXIS_RZ)
        if (kotlin.math.abs(rx) > DEAD_ZONE || kotlin.math.abs(ry) > DEAD_ZONE) {
            state.rightStick = PointF(rx, ry)
            listener?.onJoystickMoved(state, 1, rx, ry)
        }

        // Triggers (AXIS_LTRIGGER, AXIS_RTRIGGER)
        val lt = getAxisValue(event, MotionEvent.AXIS_LTRIGGER)
        val rt = getAxisValue(event, MotionEvent.AXIS_RTRIGGER)
        state.leftTrigger = lt
        state.rightTrigger = rt
        listener?.onTriggerMoved(state, 0, lt)
        listener?.onTriggerMoved(state, 1, rt)

        // DPAD
        val dpadX = getAxisValue(event, MotionEvent.AXIS_HAT_X)
        val dpadY = getAxisValue(event, MotionEvent.AXIS_HAT_Y)
        if (dpadX > 0.5f) state.buttons[KEY_DPAD_RIGHT] = true
        else if (dpadX < -0.5f) state.buttons[KEY_DPAD_LEFT] = true
        if (dpadY > 0.5f) state.buttons[KEY_DPAD_UP] = true
        else if (dpadY < -0.5f) state.buttons[KEY_DPAD_DOWN] = true

        return true
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val deviceId = event.deviceId
        if (deviceId < 0) return false
        val state = getOrCreateController(deviceId)
        state.buttons[keyCode] = true
        listener?.onButtonPressed(state, keyCode, true)
        return true
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val deviceId = event.deviceId
        if (deviceId < 0) return false
        val state = getOrCreateController(deviceId)
        state.buttons[keyCode] = false
        listener?.onButtonPressed(state, keyCode, false)
        return true
    }

    fun getController(deviceId: Int): ControllerState? = connectedControllers[deviceId]
    fun getAllControllers(): List<ControllerState> = connectedControllers.values.toList()
    fun hasController(): Boolean = connectedControllers.isNotEmpty()

    private fun getOrCreateController(id: Int): ControllerState {
        return connectedControllers.getOrPut(id) {
            val name = getDeviceName(id)
            val type = detectControllerType(name)
            val state = ControllerState(id, name, type)
            listener?.onControllerConnected(state)
            state
        }
    }

    private fun getDeviceName(id: Int): String {
        return try {
            InputDevice.getDevice(id)?.name ?: "Unknown"
        } catch (e: Exception) { "Unknown" }
    }

    private fun getAxisValue(event: MotionEvent, axis: Int): Float {
        return try { event.getAxisValue(axis) } catch (e: Exception) { 0f }
    }

    companion object {
        const val DEAD_ZONE = 0.15f
        const val KEY_A = KeyEvent.KEYCODE_BUTTON_A
        const val KEY_B = KeyEvent.KEYCODE_BUTTON_B
        const val KEY_X = KeyEvent.KEYCODE_BUTTON_X
        const val KEY_Y = KeyEvent.KEYCODE_BUTTON_Y
        const val KEY_START = KeyEvent.KEYCODE_BUTTON_START
        const val KEY_SELECT = KeyEvent.KEYCODE_BUTTON_SELECT
        const val KEY_LB = KeyEvent.KEYCODE_BUTTON_L1
        const val KEY_RB = KeyEvent.KEYCODE_BUTTON_R1
        const val KEY_LS = KeyEvent.KEYCODE_BUTTON_THUMBL
        const val KEY_RS = KeyEvent.KEYCODE_BUTTON_THUMBR
        const val KEY_DPAD_UP = KeyEvent.KEYCODE_DPAD_UP
        const val KEY_DPAD_DOWN = KeyEvent.KEYCODE_DPAD_DOWN
        const val KEY_DPAD_LEFT = KeyEvent.KEYCODE_DPAD_LEFT
        const val KEY_DPAD_RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT
        const val KEY_HOME = KeyEvent.KEYCODE_BUTTON_MODE
    }
}

