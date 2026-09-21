package com.gostudios.console.vr.games

import com.gostudios.console.vr.engine.VREngine
import kotlin.math.abs

class VRPong : VRGame() {
    override val name = "VR Pong"
    override val description = "Classic pong in VR"
    override val color = floatArrayOf(0f, 0.4f, 1f)

    private var ballX = 0f
    private var ballY = 0f
    private var ballZ = -3f
    private var ballVX = 2f
    private var ballVY = 1.5f
    private var paddleY = 0f
    private var paddleX = -1.5f
    private var aiPaddleY = 0f
    private var ballSize = 0.1f
    private var paddleWidth = 0.6f
    private var paddleHeight = 0.15f
    private var arenaW = 2f
    private var arenaH = 1.5f

    override fun init(engine: VREngine) {
        reset()
    }

    override fun reset() {
        super.reset()
        ballX = 0f; ballY = 0f; ballZ = -3f
        ballVX = 2f; ballVY = 1.5f
        paddleY = 0f; aiPaddleY = 0f
    }

    override fun update(delta: Float, engine: VREngine) {
        if (isPaused) return

        ballX += ballVX * delta
        ballY += ballVY * delta

        if (ballY > arenaH || ballY < -arenaH) ballVY = -ballVY
        ballY = ballY.coerceIn(-arenaH, arenaH)

        // Paddle collision
        if (ballX < paddleX + paddleWidth && ballX > paddleX - paddleWidth &&
            ballY < paddleY + paddleHeight && ballY > paddleY - paddleHeight) {
            ballVX = abs(ballVX)
            ballVY += paddleY * 0.5f
        }

        // AI paddle
        val aiTarget = ballY
        aiPaddleY += (aiTarget - aiPaddleY) * delta * 2f
        aiPaddleY = aiPaddleY.coerceIn(-arenaH, arenaH)

        if (ballX > 1.5f) {
            if (ballY < aiPaddleY + paddleHeight && ballY > aiPaddleY - paddleHeight) {
                ballVX = -abs(ballVX)
            } else {
                score++
                reset()
            }
        }

        if (ballX < -2f) {
            reset()
        }
    }

    override fun render(engine: VREngine, mvp: FloatArray, isLeftEye: Boolean) {
        // Ball
        engine.drawSphere(mvp, 1f, 1f, 1f, ballSize, ballX, ballY, ballZ)
        // Player paddle
        engine.drawCube(mvp, 0f, 0.4f, 1f, 0.15f, paddleX, paddleY, ballZ)
        // AI paddle
        engine.drawCube(mvp, 1f, 0.2f, 0.2f, 0.15f, 1.5f, aiPaddleY, ballZ)
        // Arena walls
        engine.drawCube(mvp, 0.3f, 0.3f, 0.5f, 0.05f, 0f, arenaH, ballZ)
        engine.drawCube(mvp, 0.3f, 0.3f, 0.5f, 0.05f, 0f, -arenaH, ballZ)
    }

    override fun onButtonPressed(button: Int, pressed: Boolean) {}
    override fun onJoystickMoved(stick: Int, x: Float, y: Float) {
        if (stick == 0) paddleY += y * 0.1f
    }
}
