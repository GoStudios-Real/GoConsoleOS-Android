package com.gostudios.console.vr.engine

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class VREngine(context: Context) : GLSurfaceView(context), GLSurfaceView.Renderer {

    private var headTracker: HeadTracker
    private val leftProjection = FloatArray(16)
    private val rightProjection = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private var screenWidth = 1920
    private var screenHeight = 1080
    private var ipd = 0.063f

    private var programId = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpHandle = 0

    private var vrScene: VRScene? = null
    private var onRenderCallback: ((Float) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY
        headTracker = HeadTracker(context)
    }

    fun setScene(scene: VRScene) { vrScene = scene }
    fun setOnRenderCallback(cb: (Float) -> Unit) { onRenderCallback = cb }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.05f, 0.05f, 0.08f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        val vertShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertShader)
        GLES20.glAttachShader(programId, fragShader)
        GLES20.glLinkProgram(programId)
        positionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        colorHandle = GLES20.glGetUniformLocation(programId, "uColor")
        mvpHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")

        vrScene?.onInit()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
        val aspect = (w / 2).toFloat() / h.toFloat()
        Matrix.perspectiveM(leftProjection, 0, 70f, aspect, 0.1f, 100f)
        Matrix.perspectiveM(rightProjection, 0, 70f, aspect, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val delta = headTracker.update()
        headTracker.getViewMatrix(viewMatrix)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(programId)

        val halfIPD = ipd / 2f
        val eyeOffset = floatArrayOf(halfIPD, 0f, 0f)
        val rightEyeOffset = floatArrayOf(-halfIPD, 0f, 0f)

        // Left eye
        GLES20.glViewport(0, 0, screenWidth / 2, screenHeight)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, eyeOffset[0], eyeOffset[1], eyeOffset[2])
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, leftProjection, 0, tempMatrix, 0)
        vrScene?.onDraw(this, mvpMatrix, delta, true)

        // Right eye
        GLES20.glViewport(screenWidth / 2, 0, screenWidth / 2, screenHeight)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, rightEyeOffset[0], rightEyeOffset[1], rightEyeOffset[2])
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, rightProjection, 0, tempMatrix, 0)
        vrScene?.onDraw(this, mvpMatrix, delta, false)

        onRenderCallback?.invoke(delta)
    }

    fun drawCube(mvp: FloatArray, r: Float, g: Float, b: Float, size: Float = 1f, x: Float = 0f, y: Float = 0f, z: Float = -3f) {
        val s = size / 2f
        val vertices = floatArrayOf(
            x-s, y+s, z+s, x+s, y+s, z+s, x+s, y-s, z+s, x-s, y-s, z+s,
            x-s, y+s, z-s, x-s, y-s, z-s, x+s, y-s, z-s, x+s, y+s, z-s,
            x-s, y+s, z-s, x-s, y+s, z+s, x-s, y-s, z+s, x-s, y-s, z-s,
            x+s, y+s, z+s, x+s, y+s, z-s, x+s, y-s, z-s, x+s, y-s, z+s,
            x-s, y+s, z-s, x+s, y+s, z-s, x+s, y+s, z+s, x-s, y+s, z+s,
            x-s, y-s, z+s, x+s, y-s, z+s, x+s, y-s, z-s, x-s, y-s, z-s
        )
        val buf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(vertices).position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glUniform4f(colorHandle, r, g, b, 1f)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 4, 4)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 8, 4)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 12, 4)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 16, 4)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 20, 4)
    }

    fun drawSphere(mvp: FloatArray, r: Float, g: Float, b: Float, radius: Float = 0.5f, x: Float = 0f, y: Float = 0f, z: Float = -3f, slices: Int = 16, stacks: Int = 8) {
        val verts = mutableListOf<Float>()
        for (i in 0 until stacks) {
            val phi1 = Math.PI * i / stacks
            val phi2 = Math.PI * (i + 1) / stacks
            for (j in 0 until slices) {
                val theta1 = 2 * Math.PI * j / slices
                val theta2 = 2 * Math.PI * (j + 1) / slices
                val p1 = sphericalToCart(phi1, theta1, radius, x, y, z)
                val p2 = sphericalToCart(phi1, theta2, radius, x, y, z)
                val p3 = sphericalToCart(phi2, theta2, radius, x, y, z)
                val p4 = sphericalToCart(phi2, theta1, radius, x, y, z)
                verts.addAll(listOf(p1[0],p1[1],p1[2], p2[0],p2[1],p2[2], p3[0],p3[1],p3[2]))
                verts.addAll(listOf(p1[0],p1[1],p1[2], p3[0],p3[1],p3[2], p4[0],p4[1],p4[2]))
            }
        }
        val arr = verts.toFloatArray()
        val buf = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(arr).position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glUniform4f(colorHandle, r, g, b, 1f)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, arr.size / 3)
    }

    private fun sphericalToCart(phi: Double, theta: Double, r: Float, cx: Float, cy: Float, cz: Float): FloatArray {
        val x = (r * sin(phi) * cos(theta) + cx).toFloat()
        val y = (r * cos(phi) + cy).toFloat()
        val z = (r * sin(phi) * sin(theta) + cz).toFloat()
        return floatArrayOf(x, y, z)
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """
    }
}

interface VRScene {
    fun onInit() {}
    fun onDraw(engine: VREngine, mvp: FloatArray, delta: Float, isLeftEye: Boolean)
}

