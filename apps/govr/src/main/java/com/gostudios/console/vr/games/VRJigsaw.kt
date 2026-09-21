package com.gostudios.console.vr.games

import com.gostudios.console.vr.engine.VREngine
import kotlin.random.Random

class VRJigsaw : VRGame() {
    override val name = "VR Jigsaw"
    override val description = "Solve the 3D puzzle"
    override val color = floatArrayOf(0.6f, 0.3f, 0.8f)

    private data class Piece(
        var x: Float, var y: Float, var z: Float,
        val targetX: Float, val targetY: Float, val targetZ: Float,
        val r: Float, val g: Float, val b: Float,
        var isPlaced: Boolean = false
    )

    private val pieces = mutableListOf<Piece>()
    private var selectedIndex = 0
    private var gridSize = 3

    override fun init(engine: VREngine) { reset() }

    override fun reset() {
        super.reset()
        pieces.clear()
        selectedIndex = 0
        val colors = arrayOf(
            floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, 0f, 1f),
            floatArrayOf(1f, 1f, 0f), floatArrayOf(1f, 0f, 1f), floatArrayOf(0f, 1f, 1f),
            floatArrayOf(1f, 0.5f, 0f), floatArrayOf(0.5f, 0f, 1f), floatArrayOf(0f, 0.5f, 0.5f)
        )
        var idx = 0
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val targetX = (col - 1) * 0.4f
                val targetY = (row - 1) * 0.4f
                val targetZ = -3f
                val c = colors[idx % colors.size]
                pieces.add(Piece(
                    Random.nextFloat() * 4 - 2,
                    Random.nextFloat() * 3 - 2,
                    Random.nextFloat() * 3 - 5f,
                    targetX, targetY, targetZ,
                    c[0], c[1], c[2]
                ))
                idx++
            }
        }
    }

    override fun update(delta: Float, engine: VREngine) {
        if (isPaused) return
        pieces.forEach { p ->
            if (!p.isPlaced) {
                val dx = p.targetX - p.x
                val dy = p.targetY - p.y
                val dz = p.targetZ - p.z
                val dist = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
                if (dist < 0.05f) {
                    p.isPlaced = true
                    score++
                }
            }
        }
    }

    override fun render(engine: VREngine, mvp: FloatArray, isLeftEye: Boolean) {
        pieces.forEachIndexed { i, p ->
            val size = 0.18f
            if (i == selectedIndex && !p.isPlaced) {
                engine.drawCube(mvp, 1f, 1f, 1f, size, p.x, p.y, p.z)
            } else {
                engine.drawCube(mvp, p.r, p.g, p.b, size, p.x, p.y, p.z)
            }
            if (!p.isPlaced) {
                // Target indicator
                engine.drawCube(mvp, p.r * 0.3f, p.g * 0.3f, p.b * 0.3f, size * 0.9f, p.targetX, p.targetY, p.targetZ)
            }
        }
    }

    override fun onButtonPressed(button: Int, pressed: Boolean) {
        if (!pressed) return
        when (button) {
            0 -> { // A button - snap to nearest target
                val p = pieces.getOrNull(selectedIndex) ?: return
                if (!p.isPlaced) {
                    p.x = p.targetX; p.y = p.targetY; p.z = p.targetZ
                }
            }
            1 -> { // B - next piece
                selectedIndex = (selectedIndex + 1) % pieces.size
            }
        }
    }

    override fun onJoystickMoved(stick: Int, x: Float, y: Float) {
        val p = pieces.getOrNull(selectedIndex) ?: return
        if (!p.isPlaced && stick == 0) {
            p.x += x * 0.02f
            p.y += y * 0.02f
        }
    }
}
