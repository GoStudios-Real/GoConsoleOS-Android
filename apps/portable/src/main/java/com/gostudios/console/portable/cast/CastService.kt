package com.gostudios.console.portable.cast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.gostudios.console.sdk.ConsoleHost
import com.gostudios.console.sdk.LinkClient
import com.gostudios.console.sdk.Protocol
import java.io.ByteArrayOutputStream
import org.json.JSONObject

/**
 * GoConsoleOS Cast sender service — captures the device screen with
 * MediaProjection and streams JPEG frames to the host over the LAN transport.
 */
class CastService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var client: LinkClient? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread("gocast").also { it.start() }
        handler = Handler(handlerThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        val hostJson = intent?.getStringExtra(EXTRA_HOST)
        if (data == null || hostJson == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val host: ConsoleHost = ConsoleHost.fromJson(JSONObject(hostJson))

        client = LinkClient(host, object : LinkClient.Listener {
            override fun onConnected(h: ConsoleHost) {
                client?.sendControl("cast.start")
            }
            override fun onControl(type: String, payload: JSONObject) {}
            override fun onFrame(type: Int, payload: ByteArray) {}
            override fun onDisconnected(error: String?) { stopSelf() }
        })
        client?.connect()

        startCapture(host, resultCode, data)
        return START_STICKY
    }

    private fun startCapture(host: ConsoleHost, resultCode: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)
        if (mediaProjection == null) { stopSelf(); return }

        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener(::onFrameAvailable, handler)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "GoCast", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler)
    }

    private fun onFrameAvailable(reader: ImageReader) {
        val image: Image? = reader.acquireLatestImage() ?: return
        try {
            val planes = image.planes
            if (planes.isEmpty()) return
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            // JPEG-encode and push over the transport.
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            client?.sendFrame(Protocol.FRAME_CAST_VIDEO, out.toByteArray())
            buffer.rewind()
        } finally {
            image.close()
        }
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        client?.close()
        handlerThread?.quitSafely()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        const val EXTRA_HOST = "hostJson"

        fun start(context: Context, resultCode: Int, data: Intent, host: ConsoleHost) {
            val i = Intent(context, CastService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
                .putExtra(EXTRA_HOST, host.toJson().toString())
            context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CastService::class.java))
        }
    }
}