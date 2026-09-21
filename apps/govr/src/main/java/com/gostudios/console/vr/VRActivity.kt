package com.gostudios.console.vr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import com.gostudios.console.vr.controller.BluetoothController
import com.gostudios.console.vr.engine.VREngine
import com.gostudios.console.vr.engine.VRScene
import com.gostudios.console.vr.games.GameActivity

class VRActivity : Activity(), BluetoothController.ControllerListener {

    private lateinit var vrEngine: VREngine
    private lateinit var controller: BluetoothController
    private var selectedGame = 0

    private val gameNames = arrayOf("VR Pong", "VR Snake", "VR Space Shooter", "VR Jigsaw", "VR Basketball")

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

        vrEngine = VREngine(this)
        vrEngine.setScene(object : VRScene {
            override fun onDraw(engine: VREngine, mvp: FloatArray, delta: Float, isLeftEye: Boolean) {
                drawMenu(engine, mvp, delta, isLeftEye)
            }
        })

        setContentView(vrEngine)
        vrEngine.onResume()

        controller = BluetoothController(this)
        controller.setListener(this)
        controller.start()
    }

    private fun drawMenu(engine: VREngine, mvp: FloatArray, delta: Float, isLeftEye: Boolean) {
        // Background sky
        engine.drawCube(mvp, 0.05f, 0.05f, 0.1f, 30f, 0f, 0f, -15f)

        // Floor
        engine.drawCube(mvp, 0.1f, 0.12f, 0.18f, 20f, 0f, -2f, -10f)

        // Title
        engine.drawCube(mvp, 0f, 0.4f, 1f, 0.5f, 0f, 2f, -4f) // Blue bar = title

        // Game cards
        for (i in gameNames.indices) {
            val x = (i - 2) * 1.5f
            val y = 0f
            val z = -3f
            val isSelected = i == selectedGame

            if (isSelected) {
                // Highlight
                engine.drawCube(mvp, 0f, 0.4f, 1f, 0.9f, x, y, z - 0.05f)
            }

            // Card
            val r = if (isSelected) 0.15f else 0.1f
            val g = if (isSelected) 0.18f else 0.13f
            val b = if (isSelected) 0.3f else 0.2f
            engine.drawCube(mvp, r, g, b, 0.8f, x, y, z)
        }

        // Info text indicators (colored blocks as "text")
        engine.drawCube(mvp, 1f, 1f, 1f, 0.3f, 0f, -1f, -3f) // Controller indicator
        engine.drawCube(mvp, 0f, 0.8f, 0f, 0.15f, 1f, -1f, -3f) // Connected indicator
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (controller.onKeyDown(keyCode, event)) return true

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> selectedGame = (selectedGame - 1 + gameNames.size) % gameNames.size
            KeyEvent.KEYCODE_DPAD_RIGHT -> selectedGame = (selectedGame + 1) % gameNames.size
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> launchGame(selectedGame)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (controller.onGenericMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    private fun launchGame(index: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("game_index", index)
        startActivity(intent)
    }

    override fun onControllerConnected(state: BluetoothController.ControllerState) {}
    override fun onControllerDisconnected(state: BluetoothController.ControllerState) {}
    override fun onButtonPressed(state: BluetoothController.ControllerState, button: Int, pressed: Boolean) {
        if (!pressed) return
        when (button) {
            BluetoothController.KEY_DPAD_LEFT -> selectedGame = (selectedGame - 1 + gameNames.size) % gameNames.size
            BluetoothController.KEY_DPAD_RIGHT -> selectedGame = (selectedGame + 1) % gameNames.size
            BluetoothController.KEY_A -> launchGame(selectedGame)
        }
    }

    override fun onJoystickMoved(state: BluetoothController.ControllerState, axis: Int, x: Float, y: Float) {
        if (axis == 0 && Math.abs(x) > 0.5f) {
            selectedGame = if (x > 0) (selectedGame + 1) % gameNames.size
            else (selectedGame - 1 + gameNames.size) % gameNames.size
        }
    }

    override fun onTriggerMoved(state: BluetoothController.ControllerState, axis: Int, value: Float) {}

    override fun onResume() { super.onResume(); vrEngine.onResume(); controller.start() }
    override fun onPause() { super.onPause(); vrEngine.onPause(); controller.stop() }
    override fun onDestroy() { super.onDestroy(); controller.stop() }
}

