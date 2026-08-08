package com.gostudios.console.portable.cast

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gostudios.console.portable.R
import com.gostudios.console.sdk.ConsoleHost
import org.json.JSONObject

/**
 * Requests the MediaProjection permission, then starts [CastService] to stream
 * the screen to the chosen host over the LAN transport.
 */
class MediaProjectionActivity : AppCompatActivity() {

    private var host: ConsoleHost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast)

        val hostJson = intent.getStringExtra(EXTRA_HOST)
        host = hostJson?.let { ConsoleHost.fromJson(JSONObject(it)) }
        findViewById<TextView>(R.id.txtCast).text =
            "Allow GoConsoleOS Cast to share your screen?"

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CAPTURE) {
            val h = host
            val d = data
            if (resultCode == RESULT_OK && h != null && d != null) {
                CastService.start(this, resultCode, d, h)
            }
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val REQUEST_CAPTURE = 7001
        private const val EXTRA_HOST = "hostJson"

        fun intent(context: Context, host: ConsoleHost): Intent =
            Intent(context, MediaProjectionActivity::class.java)
                .putExtra(EXTRA_HOST, host.toJson().toString())
    }
}