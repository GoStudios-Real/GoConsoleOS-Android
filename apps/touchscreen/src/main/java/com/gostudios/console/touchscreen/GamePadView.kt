package com.gostudios.console.touchscreen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.gostudios.console.sdk.ControllerInput
import com.gostudios.console.sdk.Protocol
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Full touch gamepad for GoConsoleOS: two analog sticks, D-pad, face buttons
 * (A/B/X/Y), shoulder + trigger pads and Start/Back. Draws itself in the
 * GoConsoleOS style and streams button/axis state to the host as FRAME_INPUT.
 */
class GamePadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onStateChanged: ((ByteArray) -> Unit)? = null

    private enum class Control { NONE, LSTICK, RSTICK, DPAD, A, B, X, Y, L1, R1, L2, R2, START, BACK }

    // geometry (recomputed on size change)
    private var w = 0f
    private var h = 0f
    private val rL2 = RectF()
    private val rR2 = RectF()
    private val rL1 = RectF()
    private val rR1 = RectF()
    private var cxDpad = 0f
    private var cyDpad = 0f
    private var dpadHalf = 0f
    private var cxLs = 0f
    private var cyLs = 0f
    private var cxRs = 0f
    private var cyRs = 0f
    private var stickR = 0f
    private var stickKnobR = 0f
    private var faceR = 0f
    private var faceCx = 0f
    private var faceCy = 0f
    private var startX = 0f
    private var startY = 0f
    private var backX = 0f
    private var backY = 0f
    private var centerR = 0f

    // input state
    private var buttons = 0L
    private var lTrigger = 0
    private var rTrigger = 0
    private var lx = 0f
    private var ly = 0f
    private var rx = 0f
    private var ry = 0f

    // touch bookkeeping: pointerId -> control
    private val assigned = HashMap<Int, Control>()

    // paints
    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E2236.toInt() }
    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2A3050.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val pActive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00C9DB.toInt() }
    private val pActiveDim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x6600C9DB.toInt() }
    private val pKnob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00C9DB.toInt() }
    private val pKnobDim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0E4A54.toInt() }
    private val pTextSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7A80A0.toInt(); textAlign = Paint.Align.CENTER; textSize = 18f
    }
    private val pFaceA = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2EA681.toInt() }
    private val pFaceB = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6514D.toInt() }
    private val pFaceX = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4C9AFF.toInt() }
    private val pFaceY = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE0AF68.toInt() }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)
        w = width.toFloat()
        h = height.toFloat()
        layoutGeometry()
    }

    private fun layoutGeometry() {
        val u = min(w, h)

        // shoulder/trigger pads along the top edge
        val padW = w * 0.22f
        val padH = u * 0.16f
        rL2.set(w * 0.02f, 0f, w * 0.02f + padW, padH)
        rR2.set(w * 0.78f - padW, 0f, w * 0.98f, padH)
        rL1.set(w * 0.02f, padH + u * 0.015f, w * 0.02f + padW * 0.8f, padH + u * 0.015f + padH * 0.9f)
        rR1.set(w * 0.98f - padW * 0.8f, padH + u * 0.015f, w * 0.98f, padH + u * 0.015f + padH * 0.9f)

        // dpad (left-mid)
        dpadHalf = u * 0.12f
        cxDpad = w * 0.17f
        cyDpad = h * 0.52f

        // face buttons (right-mid), diamond: X left, B right, Y top, A bottom
        faceR = u * 0.075f
        faceCx = w * 0.83f
        faceCy = h * 0.52f

        // sticks (bottom corners)
        stickR = u * 0.19f
        stickKnobR = u * 0.085f
        cxLs = w * 0.14f
        cyLs = h * 0.83f
        cxRs = w * 0.86f
        cyRs = h * 0.83f

        // start/back (center-mid)
        centerR = u * 0.035f
        startX = w * 0.47f
        backX = w * 0.53f
        startY = h * 0.5f
        backY = h * 0.5f
    }

    private fun faceD() = max(stickR * 0.62f, w * 0.02f)

    // ---- hit testing ----

    private fun hitControl(x: Float, y: Float): Control = when {
        rL2.contains(x, y) -> Control.L2
        rR2.contains(x, y) -> Control.R2
        rL1.contains(x, y) -> Control.L1
        rR1.contains(x, y) -> Control.R1
        inCircle(x, y, cxLs, cyLs, stickR) -> Control.LSTICK
        inCircle(x, y, cxRs, cyRs, stickR) -> Control.RSTICK
        inDpad(x, y) -> Control.DPAD
        inCircle(x, y, startX, startY, centerR * 1.7f) -> Control.START
        inCircle(x, y, backX, backY, centerR * 1.7f) -> Control.BACK
        else -> inFace(x, y) ?: Control.NONE
    }

    private fun inFace(x: Float, y: Float): Control? {
        val d = faceD()
        if (inCircle(x, y, faceCx + d, faceCy, faceR)) return Control.B
        if (inCircle(x, y, faceCx, faceCy + d, faceR)) return Control.A
        if (inCircle(x, y, faceCx, faceCy - d, faceR)) return Control.Y
        if (inCircle(x, y, faceCx - d, faceCy, faceR)) return Control.X
        return null
    }

    private fun inDpad(x: Float, y: Float): Boolean {
        val b = dpadHalf * 0.34f
        return (abs(x - cxDpad) <= b && abs(y - cyDpad) <= dpadHalf) ||
            (abs(y - cyDpad) <= b && abs(x - cxDpad) <= dpadHalf)
    }

    private fun inCircle(x: Float, y: Float, cx: Float, cy: Float, r: Float) =
        hypot(x - cx, y - cy) <= r

    // ---- touch handling ----

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                val c = hitControl(x, y)
                if (c != Control.NONE) {
                    assigned[pid] = c
                    when (c) {
                        Control.LSTICK, Control.RSTICK -> updateStick(pid, x, y)
                        Control.DPAD -> updateDpad(x, y, true)
                        else -> setButton(c, true)
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val c = assigned[pid] ?: continue
                    when (c) {
                        Control.LSTICK, Control.RSTICK -> updateStick(pid, event.getX(i), event.getY(i))
                        Control.DPAD -> updateDpad(event.getX(i), event.getY(i), false)
                        else -> Unit
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = if (event.actionMasked == MotionEvent.ACTION_CANCEL) 0 else event.actionIndex
                val pid = event.getPointerId(idx)
                val c = assigned.remove(pid)
                if (c != null) {
                    when (c) {
                        Control.LSTICK -> { lx = 0f; ly = 0f }
                        Control.RSTICK -> { rx = 0f; ry = 0f }
                        Control.DPAD -> updateDpad(event.getX(idx), event.getY(idx), false, release = true)
                        else -> setButton(c, false)
                    }
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateStick(pid: Int, x: Float, y: Float) {
        val c = assigned[pid]
        if (c == Control.LSTICK) {
            lx = clamp((x - cxLs) / stickR, -1f, 1f)
            ly = clamp((y - cyLs) / stickR, -1f, 1f)
        } else if (c == Control.RSTICK) {
            rx = clamp((x - cxRs) / stickR, -1f, 1f)
            ry = clamp((y - cyRs) / stickR, -1f, 1f)
        }
        pushState()
    }

    /**
     * D-pad is directional: the pressed direction follows where the finger is
     * relative to the pad centre. [release] clears it entirely (finger up).
     */
    private fun updateDpad(x: Float, y: Float, initial: Boolean, release: Boolean = false) {
        val prev = buttons
        buttons = buttons and (
            Protocol.BTN_DPAD_UP or Protocol.BTN_DPAD_DOWN or
                Protocol.BTN_DPAD_LEFT or Protocol.BTN_DPAD_RIGHT
            ).inv()
        if (!release) {
            val dx = x - cxDpad
            val dy = y - cyDpad
            if (abs(dx) > abs(dy)) {
                if (dx > 0) buttons = buttons or Protocol.BTN_DPAD_RIGHT
                else buttons = buttons or Protocol.BTN_DPAD_LEFT
            } else {
                if (dy > 0) buttons = buttons or Protocol.BTN_DPAD_DOWN
                else buttons = buttons or Protocol.BTN_DPAD_UP
            }
        }
        if (buttons != prev) haptic()
        pushState()
    }

    private fun clamp(v: Float, lo: Float, hi: Float) = max(lo, min(hi, v))

    private fun setButton(c: Control, down: Boolean) {
        val bit = when (c) {
            Control.A -> Protocol.BTN_A
            Control.B -> Protocol.BTN_B
            Control.X -> Protocol.BTN_X
            Control.Y -> Protocol.BTN_Y
            Control.L1 -> Protocol.BTN_LEFT_SHOULDER
            Control.R1 -> Protocol.BTN_RIGHT_SHOULDER
            Control.L2 -> Protocol.BTN_LEFT_TRIGGER
            Control.R2 -> Protocol.BTN_RIGHT_TRIGGER
            Control.START -> Protocol.BTN_START
            Control.BACK -> Protocol.BTN_BACK
            else -> return
        }
        buttons = if (down) buttons or bit else buttons and bit.inv()
        if (c == Control.L2) lTrigger = if (down) 255 else 0
        if (c == Control.R2) rTrigger = if (down) 255 else 0
        haptic()
        pushState()
    }

    // ---- drawing ----

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFF0A0F1E.toInt())
        drawTopPads(canvas)
        drawDpad(canvas)
        drawStick(canvas, cxLs, cyLs, lx, ly)
        drawStick(canvas, cxRs, cyRs, rx, ry)
        drawFace(canvas)
        drawCenter(canvas)
    }

    private fun drawTopPads(canvas: Canvas) {
        drawPad(canvas, rL2, "L2", isOn(Control.L2))
        drawPad(canvas, rR2, "R2", isOn(Control.R2))
        drawPad(canvas, rL1, "L1", isOn(Control.L1))
        drawPad(canvas, rR1, "R1", isOn(Control.R1))
    }

    private fun drawPad(canvas: Canvas, r: RectF, label: String, on: Boolean) {
        canvas.drawRoundRect(r, 14f, 14f, if (on) pActive else pFill)
        canvas.drawRoundRect(r, 14f, 14f, pStroke)
        pTextSmall.textSize = 22f
        canvas.drawText(label, r.centerX(), r.centerY() + 8f, pTextSmall)
    }

    private fun isOn(c: Control): Boolean = when (c) {
        Control.L2 -> (buttons and Protocol.BTN_LEFT_TRIGGER) != 0L
        Control.R2 -> (buttons and Protocol.BTN_RIGHT_TRIGGER) != 0L
        Control.L1 -> (buttons and Protocol.BTN_LEFT_SHOULDER) != 0L
        Control.R1 -> (buttons and Protocol.BTN_RIGHT_SHOULDER) != 0L
        Control.A -> (buttons and Protocol.BTN_A) != 0L
        Control.B -> (buttons and Protocol.BTN_B) != 0L
        Control.X -> (buttons and Protocol.BTN_X) != 0L
        Control.Y -> (buttons and Protocol.BTN_Y) != 0L
        Control.START -> (buttons and Protocol.BTN_START) != 0L
        Control.BACK -> (buttons and Protocol.BTN_BACK) != 0L
        else -> false
    }

    private fun drawDpad(canvas: Canvas) {
        val b = dpadHalf * 0.34f
        val up = (buttons and Protocol.BTN_DPAD_UP) != 0L
        val down = (buttons and Protocol.BTN_DPAD_DOWN) != 0L
        val left = (buttons and Protocol.BTN_DPAD_LEFT) != 0L
        val right = (buttons and Protocol.BTN_DPAD_RIGHT) != 0L
        drawDpadBar(canvas, cxDpad - b, cyDpad - dpadHalf, cxDpad + b, cyDpad, up)
        drawDpadBar(canvas, cxDpad - b, cyDpad, cxDpad + b, cyDpad + dpadHalf, down)
        drawDpadBar(canvas, cxDpad - dpadHalf, cyDpad - b, cxDpad, cyDpad + b, left)
        drawDpadBar(canvas, cxDpad, cyDpad - b, cxDpad + dpadHalf, cyDpad + b, right)
    }

    private fun drawDpadBar(canvas: Canvas, l: Float, t: Float, rr: Float, b: Float, on: Boolean) {
        val r = RectF(l, t, rr, b)
        canvas.drawRoundRect(r, 8f, 8f, if (on) pActive else pFill)
        canvas.drawRoundRect(r, 8f, 8f, pStroke)
    }

    private fun drawStick(canvas: Canvas, cx: Float, cy: Float, ax: Float, ay: Float) {
        canvas.drawCircle(cx, cy, stickR, pFill)
        canvas.drawCircle(cx, cy, stickR, pStroke)
        if (ax != 0f || ay != 0f) canvas.drawCircle(cx, cy, stickR, pActiveDim)
        val kx = cx + ax * stickR * 0.55f
        val ky = cy + ay * stickR * 0.55f
        canvas.drawCircle(kx, ky, stickKnobR, pKnob)
        canvas.drawCircle(kx, ky, stickKnobR, pKnobDim)
        canvas.drawCircle(cx, cy, 6f, pKnobDim)
    }

    private fun drawFace(canvas: Canvas) {
        val d = faceD()
        drawFaceBtn(canvas, faceCx - d, faceCy, "X", pFaceX, isOn(Control.X))
        drawFaceBtn(canvas, faceCx, faceCy - d, "Y", pFaceY, isOn(Control.Y))
        drawFaceBtn(canvas, faceCx + d, faceCy, "B", pFaceB, isOn(Control.B))
        drawFaceBtn(canvas, faceCx, faceCy + d, "A", pFaceA, isOn(Control.A))
    }

    private fun drawFaceBtn(canvas: Canvas, cx: Float, cy: Float, label: String, p: Paint, on: Boolean) {
        canvas.drawCircle(cx, cy, faceR, if (on) pActive else pFill)
        if (on) canvas.drawCircle(cx, cy, faceR, pActiveDim)
        canvas.drawCircle(cx, cy, faceR, pStroke)
        val l = Paint(p).apply { textAlign = Paint.Align.CENTER }
        l.textSize = faceR * 0.9f
        canvas.drawText(label, cx, cy + faceR * 0.32f, l)
    }

    private fun drawCenter(canvas: Canvas) {
        val s = (buttons and Protocol.BTN_START) != 0L
        val b = (buttons and Protocol.BTN_BACK) != 0L
        canvas.drawCircle(startX, startY, centerR, if (s) pActive else pFill)
        canvas.drawCircle(startX, startY, centerR, pStroke)
        canvas.drawCircle(backX, backY, centerR, if (b) pActive else pFill)
        canvas.drawCircle(backX, backY, centerR, pStroke)
    }

    private fun haptic() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun pushState() {
        val frame = ControllerInput.pack(
            buttons,
            lTrigger,
            rTrigger,
            (lx * 32767).toInt(),
            (ly * 32767).toInt(),
            (rx * 32767).toInt(),
            (ry * 32767).toInt(),
        )
        onStateChanged?.invoke(frame)
    }
}
