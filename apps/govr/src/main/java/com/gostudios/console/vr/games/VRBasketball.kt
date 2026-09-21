package com.gostudios.console.vr.games

import com.gostudios.console.vr.engine.VREngine

class VRBasketball : VRGame() {
    override val name = "VR Basketball"
    override val description = "Throw balls at the hoop"
    override val color = floatArrayOf(1f, 0.6f, 0f)

    private data class Ball(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float, var vz: Float,
        var life: Float = 3f
    )

    private val balls = mutableListOf<Ball>()
    private var hoopY = 1.5f
    private var hoopX = 0f
    private var hoopZ = -5f
    private var throwCooldown = 0f
    private var aimX = 0f
    private var aimY = 0f

    override fun init(engine: VREngine) { reset() }

    override fun reset() {
        super.reset()
        balls.clear()
        throwCooldown = 0f
    }

    override fun update(delta: Float, engine: VREngine) {
        if (isPaused) return
        throwCooldown -= delta

        balls.forEach { b ->
            b.vy -= 4f * delta // gravity
            b.x += b.vx * delta
            b.y += b.vy * delta
            b.z += b.vz * delta
            b.life -= delta

            // Score check
            if (Math.abs(b.x - hoopX) < 0.2f && Math.abs(b.y - hoopY) < 0.3f && Math.abs(b.z - hoopZ) < 0.3f && b.vy < 0) {
                score += 10
                b.life = 0f
            }
        }
        balls.removeAll { it.life <= 0 || it.y < -2f }
    }

    override fun render(engine: VREngine, mvp: FloatArray, isLeftEye: Boolean) {
        // Hoop rim
        engine.drawCube(mvp, 1f, 0.3f, 0f, 0.05f, hoopX - 0.2f, hoopY, hoopZ)
        engine.drawCube(mvp, 1f, 0.3f, 0f, 0.05f, hoopX + 0.2f, hoopY, hoopZ)
        engine.drawCube(mvp, 1f, 0.3f, 0f, 0.05f, hoopX, hoopY, hoopZ - 0.2f)
        engine.drawCube(mvp, 1f, 0.3f, 0f, 0.05f, hoopX, hoopY, hoopZ + 0.2f)
        // Backboard
        engine.drawCube(mvp, 0.8f, 0.8f, 0.8f, 0.6f, hoopX, hoopY + 0.5f, hoopZ - 0.3f)
        // Pole
        engine.drawCube(mvp, 0.5f, 0.5f, 0.5f, 0.08f, hoopX, 0f, hoopZ - 0.3f)
        // Balls
        for (b in balls) engine.drawSphere(mvp, 0.8f, 0.4f, 0f, 0.12f, b.x, b.y, b.z)
    }

    override fun onButtonPressed(button: Int, pressed: Boolean) {
        if (pressed && throwCooldown <= 0) {
            balls.add(Ball(0f, 0.5f, -2f, aimX * 3f, 5f, -6f))
            throwCooldown = 0.3f
        }
    }

    override fun onJoystickMoved(stick: Int, x: Float, y: Float) {
        if (stick == 0) { aimX += x * 0.05f }
    }

    override fun onTriggerMoved(trigger: Int, value: Float) {
        if (trigger == 1 && throwCooldown <= 0) {
            balls.add(Ball(0f, 0.5f, -2f, aimX * 3f, 5f + value * 2f, -6f))
            throwCooldown = 0.3f
        }
    }
}
