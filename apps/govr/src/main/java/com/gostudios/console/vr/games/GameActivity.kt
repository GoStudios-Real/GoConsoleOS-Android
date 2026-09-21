package com.gostudios.console.vr.games

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import com.gostudios.console.vr.controller.BluetoothController
import com.gostudios.console.vr.engine.VREngine
import com.gostudios.console.vr.engine.VRScene

class GameActivity : Activity(), BluetoothController.ControllerListener {

    private lateinit var vrEngine: VREngine
    private lateinit var controller: BluetoothController
    private var currentGame: VRGame? = null

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

        val gameIndex = intent.getIntExtra("game_index", 0)
        currentGame = createGame(gameIndex)

        vrEngine = VREngine(this)
        vrEngine.setScene(object : VRScene {
            override fun onDraw(engine: VREngine, mvp: FloatArray, delta: Float, isLeftEye: Boolean) {
                currentGame?.update(delta, engine)
                currentGame?.render(engine, mvp, isLeftEye)
            }
        })

        setContentView(vrEngine)
        vrEngine.onResume()

        controller = BluetoothController(this)
        controller.setListener(this)
        controller.start()
    }

    private fun createGame(index: Int): VRGame = when (index) {
        0 -> VRPong()
        1 -> VRSnake()
        2 -> VRSpaceShooter()
        3 -> VRJigsaw()
        4 -> VRBasketball()
        else -> VRPong()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (controller.onKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (controller.onGenericMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    override fun onControllerConnected(state: BluetoothController.ControllerState) {}
    override fun onControllerDisconnected(state: BluetoothController.ControllerState) {}
    override fun onButtonPressed(state: BluetoothController.ControllerState, button: Int, pressed: Boolean) {
        when (button) {
            BluetoothController.KEY_A -> currentGame?.onButtonPressed(0, pressed)
            BluetoothController.KEY_B -> currentGame?.onButtonPressed(1, pressed)
            BluetoothController.KEY_X -> currentGame?.onButtonPressed(2, pressed)
            BluetoothController.KEY_Y -> currentGame?.onButtonPressed(3, pressed)
            BluetoothController.KEY_START -> currentGame?.isPaused = !(currentGame?.isPaused ?: false)
            BluetoothController.KEY_SELECT -> { currentGame?.reset() }
        }
    }

    override fun onJoystickMoved(state: BluetoothController.ControllerState, axis: Int, x: Float, y: Float) {
        currentGame?.onJoystickMoved(axis, x, y)
    }

    override fun onTriggerMoved(state: BluetoothController.ControllerState, axis: Int, value: Float) {
        currentGame?.onTriggerMoved(axis, value)
    }

    override fun onResume() { super.onResume(); vrEngine.onResume(); controller.start() }
    override fun onPause() { super.onPause(); vrEngine.onPause(); controller.stop() }
    override fun onDestroy() { super.onDestroy(); currentGame?.cleanup(); controller.stop() }
}

