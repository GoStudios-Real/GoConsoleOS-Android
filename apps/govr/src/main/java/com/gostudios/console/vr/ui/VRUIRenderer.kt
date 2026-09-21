package com.gostudios.console.vr.ui

import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VRUIRenderer {

    private var textTextureId = 0
    private var canvasBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
    }
    private var bgPaint = Paint().apply {
        color = Color.parseColor("#1E2D42")
        style = Paint.Style.FILL
    }
    private var accentPaint = Paint().apply {
        color = Color.parseColor("#0066FF")
        style = Paint.Style.FILL
    }

    data class VRButton(
        val text: String,
        var x: Float, var y: Float,
        var width: Float, var height: Float,
        val color: Int = Color.parseColor("#0066FF"),
        var isHovered: Boolean = false,
        var isPressed: Boolean = false,
        val onClick: () -> Unit = {}
    )

    private val buttons = mutableListOf<VRButton>()
    private var selectedButtonIndex = 0
    private var lookDirection = floatArrayOf(0f, 0f)

    fun addButton(text: String, x: Float, y: Float, w: Float, h: Float, color: Int = Color.parseColor("#0066FF"), onClick: () -> Unit = {}) {
        buttons.add(VRButton(text, x, y, w, h, color, onClick = onClick))
    }

    fun clearButtons() { buttons.clear(); selectedButtonIndex = 0 }

    fun updateLookDirection(yaw: Float, pitch: Float) {
        lookDirection = floatArrayOf(yaw, pitch)
        updateHover()
    }

    private fun updateHover() {
        val centerX = 540f
        val centerY = 360f
        val lookX = centerX + lookDirection[0] * 300f
        val lookY = centerY + lookDirection[1] * 200f

        buttons.forEachIndexed { i, btn ->
            btn.isHovered = lookX >= btn.x && lookX <= btn.x + btn.width &&
                    lookY >= btn.y && lookY <= btn.y + btn.height
            if (btn.isHovered) selectedButtonIndex = i
        }
    }

    fun renderOverlay(engine: com.gostudios.console.vr.engine.VREngine, mvp: FloatArray, w: Int, h: Int) {
        canvasBitmap?.recycle()
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)

        // Draw menu background panel
        val panelRect = RectF(50f, 50f, w - 50f, h - 50f)
        canvas?.drawRoundRect(panelRect, 20f, 20f, bgPaint)

        // Draw title
        paint.color = Color.parseColor("#0066FF")
        paint.textSize = 56f
        canvas?.drawText("GoConsole VR", 100f, 130f, paint)

        paint.color = Color.parseColor("#7A80A0")
        paint.textSize = 32f
        canvas?.drawText("Select a game to play", 100f, 175f, paint)

        // Draw buttons
        buttons.forEach { btn ->
            val rect = RectF(btn.x, btn.y, btn.x + btn.width, btn.y + btn.height)
            val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (btn.isHovered) Color.parseColor("#3385FF") else btn.color
                style = Paint.Style.FILL
            }
            canvas?.drawRoundRect(rect, 12f, 12f, btnPaint)

            if (btn.isHovered) {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas?.drawRoundRect(rect, 12f, 12f, borderPaint)
            }

            paint.color = Color.WHITE
            paint.textSize = 36f
            paint.textAlign = Paint.Align.CENTER
            canvas?.drawText(btn.text, btn.x + btn.width / 2, btn.y + btn.height / 2 + 12, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        uploadTexture()
    }

    private fun uploadTexture() {
        val bitmap = canvasBitmap ?: return
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        textTextureId = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    fun getSelectedButton(): VRButton? = buttons.getOrNull(selectedButtonIndex)
    fun selectNext() { selectedButtonIndex = (selectedButtonIndex + 1) % buttons.size.coerceAtLeast(1) }
    fun selectPrev() { selectedButtonIndex = (selectedButtonIndex - 1 + buttons.size) % buttons.size.coerceAtLeast(1) }
    fun activateSelected() { getSelectedButton()?.onClick?.invoke() }
}
