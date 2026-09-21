package com.gostudios.console.vr.games

import com.gostudios.console.vr.engine.VREngine
import kotlin.random.Random

class VRSnake : VRGame() {
    override val name = "VR Snake"
    override val description = "Eat food to grow"
    override val color = floatArrayOf(0f, 0.8f, 0.4f)

    private data class Segment(var x: Float, var z: Float)
    private val segments = mutableListOf<Segment>()
    private var foodX = 0f
    private var foodZ = 0f
    private var dirX = 1f
    private var dirZ = 0f
    private var moveTimer = 0f
    private var moveInterval = 0.2f
    private val gridSize = 0.3f
    private val gridHalf = 5

    override fun init(engine: VREngine) { reset() }

    override fun reset() {
        super.reset()
        segments.clear()
        segments.add(Segment(0f, -3f))
        segments.add(Segment(-gridSize, -3f))
        segments.add(Segment(-gridSize * 2, -3f))
        dirX = 1f; dirZ = 0f; moveTimer = 0f
        spawnFood()
    }

    private fun spawnFood() {
        foodX = (Random.nextInt(-gridHalf, gridHalf) * gridSize)
        foodZ = -3f + (Random.nextInt(-gridHalf, gridHalf) * gridSize)
    }

    override fun update(delta: Float, engine: VREngine) {
        if (isPaused) return
        moveTimer += delta
        if (moveTimer < moveInterval) return
        moveTimer = 0f

        val head = segments.first()
        val newX = head.x + dirX * gridSize
        val newZ = head.z + dirZ * gridSize

        // Wall collision
        if (newX < -gridHalf * gridSize || newX > gridHalf * gridSize ||
            newZ < -3f - gridHalf * gridSize || newZ > -3f + gridHalf * gridSize) {
            reset(); return
        }

        // Self collision
        for (i in 1 until segments.size) {
            if (Math.abs(segments[i].x - newX) < 0.01f && Math.abs(segments[i].z - newZ) < 0.01f) {
                reset(); return
            }
        }

        segments.add(0, Segment(newX, newZ))

        // Eat food
        if (Math.abs(newX - foodX) < gridSize && Math.abs(newZ - foodZ) < gridSize) {
            score += 10
            moveInterval = (moveInterval - 0.005f).coerceAtLeast(0.08f)
            spawnFood()
        } else {
            segments.removeAt(segments.lastIndex)
        }
    }

    override fun render(engine: VREngine, mvp: FloatArray, isLeftEye: Boolean) {
        // Snake body
        segments.forEachIndexed { i, s ->
            val brightness = 1f - (i.toFloat() / segments.size) * 0.5f
            engine.drawCube(mvp, 0f, 0.8f * brightness, 0.4f * brightness, gridSize * 0.9f, s.x, 0f, s.z)
        }
        // Food
        engine.drawSphere(mvp, 1f, 0.2f, 0.2f, gridSize * 0.6f, foodX, 0f, foodZ)
    }

    override fun onButtonPressed(button: Int, pressed: Boolean) {}
    override fun onJoystickMoved(stick: Int, x: Float, y: Float) {
        if (stick == 0) {
            if (Math.abs(x) > Math.abs(y)) {
                if (x > 0.3f && dirX != -1f) { dirX = 1f; dirZ = 0f }
                else if (x < -0.3f && dirX != 1f) { dirX = -1f; dirZ = 0f }
            } else {
                if (y > 0.3f && dirZ != 1f) { dirX = 0f; dirZ = -1f }
                else if (y < -0.3f && dirZ != -1f) { dirX = 0f; dirZ = 1f }
            }
        }
    }
}
