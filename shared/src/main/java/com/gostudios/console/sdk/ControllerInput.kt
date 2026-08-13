package com.gostudios.console.sdk

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Packs phone controller state into a FRAME_INPUT payload.
 *
 * Layout (14 bytes, all big-endian):
 *   bytes  0..3  button bitmask  (see [Protocol.BTN_*] constants)
 *   byte       4  left trigger   0..255
 *   byte       5  right trigger  0..255
 *   bytes  6..7  left stick X   int16 (-32768..32767)
 *   bytes  8..9  left stick Y   int16
 *   bytes 10..11 right stick X  int16
 *   bytes 12..13 right stick Y  int16
 */
object ControllerInput {

    const val SIZE = Protocol.INPUT_SIZE

    fun pack(
        buttons: Long,
        leftTrigger: Int = 0,
        rightTrigger: Int = 0,
        leftX: Int = 0,
        leftY: Int = 0,
        rightX: Int = 0,
        rightY: Int = 0,
    ): ByteArray {
        val buf = ByteBuffer.allocate(SIZE).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(buttons.toInt())
        buf.put(leftTrigger.coerceIn(0, 255).toByte())
        buf.put(rightTrigger.coerceIn(0, 255).toByte())
        buf.putShort(leftX.coerceIn(-32768, 32767).toShort())
        buf.putShort(leftY.coerceIn(-32768, 32767).toShort())
        buf.putShort(rightX.coerceIn(-32768, 32767).toShort())
        buf.putShort(rightY.coerceIn(-32768, 32767).toShort())
        return buf.array()
    }
}
