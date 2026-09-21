package com.gostudios.console.vr.games

import com.gostudios.console.vr.engine.VREngine
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class VRSpaceShooter : VRGame() {
    override val name = "VR Space Shooter"
    override val description = "Shoot aliens in space"
    override val color = floatArrayOf(1f, 0.3f, 0f)

    private var shipX = 0f
    private var shipY = 0f
    private var bullets = mutableListOf<Bullet>()
    private var enemies = mutableListOf<Enemy>()
    private var stars = mutableListOf<Star>()
    private var lastSpawn = 0f
    private var shootCooldown = 0f

    data class Bullet(var x: Float, var y: Float, var z: Float, var vy: Float = 0f) {
        var life = 2f
    }
    data class Enemy(var x: Float, var y: Float, var z: Float, var speed: Float = 1f) {
        var health = 1
        var size = 0.2f
    }
    data class Star(var x: Float, var y: Float, var z: Float, var speed: Float)

    override fun init(engine: VREngine) {
        reset()
        stars.clear()
        for (i in 0..50) {
            stars.add(Star(
                Random.nextFloat() * 4 - 2,
                Random.nextFloat() * 4 - 2,
                -Random.nextFloat() * 20,
                Random.nextFloat() * 2 + 1f
            ))
        }
    }

    override fun reset() {
        super.reset()
        shipX = 0f; shipY = 0f
        bullets.clear(); enemies.clear()
        lastSpawn = 0f; shootCooldown = 0f
    }

    override fun update(delta: Float, engine: VREngine) {
        if (isPaused) return
        shootCooldown -= delta
        lastSpawn += delta

        // Move bullets
        bullets.forEach { it.z -= 5f * delta; it.life -= delta }
        bullets.removeAll { it.life <= 0 || it.z < -30f }

        // Move enemies
        enemies.forEach { it.z += it.speed * delta }
        enemies.removeAll { it.z > -1f }

        // Spawn enemies
        if (lastSpawn > 1.5f) {
            lastSpawn = 0f
            enemies.add(Enemy(
                Random.nextFloat() * 3 - 1.5f,
                Random.nextFloat() * 2 - 1f,
                -15f,
                Random.nextFloat() + 0.5f
            ))
        }

        // Collision detection
        val hitEnemies = mutableListOf<Enemy>()
        val hitBullets = mutableListOf<Bullet>()
        for (bullet in bullets) {
            for (enemy in enemies) {
                if (abs(bullet.x - enemy.x) < enemy.size && abs(bullet.y - enemy.y) < enemy.size &&
                    abs(bullet.z - enemy.z) < 0.5f) {
                    enemy.health--
                    if (enemy.health <= 0) { hitEnemies.add(enemy); score += 10 }
                    hitBullets.add(bullet)
                }
            }
        }
        enemies.removeAll(hitEnemies)
        bullets.removeAll(hitBullets)

        // Move stars
        stars.forEach {
            it.z += it.speed * delta
            if (it.z > 0) { it.z = -20f; it.x = Random.nextFloat() * 4 - 2; it.y = Random.nextFloat() * 4 - 2 }
        }
    }

    override fun render(engine: VREngine, mvp: FloatArray, isLeftEye: Boolean) {
        // Ship
        engine.drawCube(mvp, 0f, 0.8f, 1f, 0.3f, shipX, shipY, -3f)
        // Bullets
        for (b in bullets) engine.drawCube(mvp, 1f, 1f, 0f, 0.05f, b.x, b.y, b.z)
        // Enemies
        for (e in enemies) engine.drawSphere(mvp, 1f, 0.2f, 0.2f, e.size, e.x, e.y, e.z)
        // Stars
        for (s in stars) engine.drawSphere(mvp, 1f, 1f, 1f, 0.02f, s.x, s.y, s.z)
    }

    override fun onButtonPressed(button: Int, pressed: Boolean) {
        if (pressed && shootCooldown <= 0) {
            bullets.add(Bullet(shipX, shipY, -3f))
            shootCooldown = 0.15f
        }
    }

    override fun onJoystickMoved(stick: Int, x: Float, y: Float) {
        if (stick == 0) { shipX += x * 0.1f; shipY += y * 0.1f }
    }

    private fun abs(f: Float) = if (f < 0) -f else f
}
