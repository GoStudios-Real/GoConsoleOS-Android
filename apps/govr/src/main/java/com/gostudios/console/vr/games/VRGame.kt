package com.gostudios.console.vr.games

import com.gostudios.console.vr.engine.VREngine

abstract class VRGame {
    abstract val name: String
    abstract val description: String
    abstract val color: FloatArray

    open var isActive = false
    open var score = 0
    open var isPaused = false

    abstract fun init(engine: VREngine)
    abstract fun update(delta: Float, engine: VREngine)
    abstract fun render(engine: VREngine, mvp: FloatArray, isLeftEye: Boolean)
    abstract fun onButtonPressed(button: Int, pressed: Boolean)
    abstract fun onJoystickMoved(stick: Int, x: Float, y: Float)
    open fun onTriggerMoved(trigger: Int, value: Float) {}
    open fun cleanup() {}
    open fun reset() { score = 0 }
}
