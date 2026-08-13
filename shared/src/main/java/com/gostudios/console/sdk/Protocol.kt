package com.gostudios.console.sdk

/**
 * Shared protocol + transport model used by all GoConsoleOS Android apps.
 *
 * The host (GoConsoleOS on Windows / GoConsoleOS Link server) listens on a
 * plain TCP socket. Every control message is one JSON line; binary frames
 * (screenshots / audio chunks / cast packets) are prefixed with a 4-byte
 * big-endian length and a 1-byte type id.
 */
object Protocol {
    const val MAGIC = "GCS"
    const val VERSION = "1.2.0"

    /** Default UDP broadcast / discovery port. */
    const val DISCOVERY_PORT = 39100

    /** Default TCP streaming port. */
    const val LINK_PORT = 39101

    // Frame type ids (1 byte) after the 4-byte length prefix.
    const val FRAME_HELLO = 0
    const val FRAME_SCREENSHOT_JPEG = 1
    const val FRAME_USB_HEALTH = 2
    const val FRAME_CAST_VIDEO = 3
    const val FRAME_CAST_AUDIO = 4
    const val FRAME_INPUT = 5

    // JSON line kinds.
    const val MSG_HELLO = "hello"
    const val MSG_LIST_DEVICES = "devices"
    const val MSG_PAIR = "pair"
    const val MSG_STREAM_START = "stream.start"
    const val MSG_STREAM_STOP = "stream.stop"
    const val MSG_USB_LIST = "usb.list"
    const val MSG_CAST_START = "cast.start"
    const val MSG_CAST_STOP = "cast.stop"

    /** List remote host tools (usb-installer, usb-health, cast, goai, ...). */
    const val MSG_TOOLS_LIST = "tools.list"

    /** Run a remote host tool by id. */
    const val MSG_TOOLS_RUN = "tools.run"

    const val USB_HEALTH_OK = "ok"
    const val USB_HEALTH_FAIR = "fair"
    const val USB_HEALTH_POOR = "poor"
    const val USB_HEALTH_UNKNOWN = "unknown"

    // ---- FRAME_INPUT (phone -> host, type 5) ----
    // Payload: 4-byte big-endian button bitmask + 2 analog trigger bytes +
    // 4 analog stick int16s (big-endian). See docs/PROTOCOL.md "Input frame".
    const val INPUT_SIZE = 14

    const val BTN_GUIDE = 1L
    const val BTN_BACK = 2L
    const val BTN_START = 4L
    const val BTN_A = 8L
    const val BTN_B = 16L
    const val BTN_X = 32L
    const val BTN_Y = 64L
    const val BTN_LEFT_SHOULDER = 128L
    const val BTN_RIGHT_SHOULDER = 256L
    const val BTN_DPAD_UP = 512L
    const val BTN_DPAD_DOWN = 1024L
    const val BTN_DPAD_LEFT = 2048L
    const val BTN_DPAD_RIGHT = 4096L
    const val BTN_LEFT_TRIGGER = 8192L
    const val BTN_RIGHT_TRIGGER = 16384L
}