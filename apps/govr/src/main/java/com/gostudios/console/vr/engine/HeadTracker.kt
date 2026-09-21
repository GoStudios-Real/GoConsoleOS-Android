package com.gostudios.console.vr.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import kotlin.math.*

class HeadTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(16)
    private val adjustedRotationMatrix = FloatArray(16)
    private val orientation = FloatArray(3)
    private val viewMatrix = FloatArray(16)

    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private var lastGyroX = 0f
    private var lastGyroY = 0f
    private var gyroX = 0f
    private var gyroY = 0f
    private var useRotationVector = true

    private var lastTimestamp = 0L
    private var sensitivity = 1.0f

    fun start() {
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            useRotationVector = true
        } ?: run {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            useRotationVector = false
        }
    }

    fun stop() { sensorManager.unregisterListener(this) }

    fun setSensitivity(s: Float) { sensitivity = s }

    fun update(): Float {
        val now = System.nanoTime()
        val delta = if (lastTimestamp > 0) (now - lastTimestamp) / 1_000_000_000f else 0.016f
        lastTimestamp = now

        if (!useRotationVector) {
            yaw += gyroX * delta * sensitivity
            pitch += gyroY * delta * sensitivity
            pitch = pitch.coerceIn(-89f, 89f)
        }

        return delta
    }

    fun getViewMatrix(out: FloatArray) {
        if (useRotationVector) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, orientation)
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, adjustedRotationMatrix)
            Matrix.setLookAtM(out, 0,
                0f, 0f, 0f,
                -adjustedRotationMatrix[2], -adjustedRotationMatrix[6], -adjustedRotationMatrix[10],
                adjustedRotationMatrix[1], adjustedRotationMatrix[5], adjustedRotationMatrix[9]
            )
        } else {
            Matrix.setRotateM(out, 0, yaw, 0f, 1f, 0f)
            val temp = FloatArray(16)
            Matrix.setRotateM(temp, 0, pitch, 1f, 0f, 0f)
            Matrix.multiplyMM(out, 0, out, 0, temp, 0)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                System.arraycopy(event.values, 0, orientation, 0, event.values.size.coerceAtMost(3))
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]
                gyroY = event.values[1]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Complementary filter with accelerometer for drift correction
                val alpha = 0.98f
                val ax = event.values[0]
                val ay = event.values[1]
                val accPitch = (Math.toDegrees(atan2(ay.toDouble(), sqrt((ax * ax).toDouble())).toFloat().toDouble())).toFloat()
                pitch = alpha * pitch + (1 - alpha) * accPitch
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
