package me.daemon.screenrecorder.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var started = false

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.FOREGROUND_SERVICE
                    ),
                    2
                )
                return@setOnClickListener
            }

            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)

            if (!started) {
                val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                startActivityForResult(captureIntent, 1)
            } else {
                stopService(Intent(this@MainActivity, ScreenRecordService::class.java))
            }

            started = !started
        }
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (1 == requestCode && Activity.RESULT_OK == resultCode) {
            val intent2 = Intent(this@MainActivity, ScreenRecordService::class.java)
            intent2.putExtra("resultCode", resultCode);
            intent2.putExtra("data", data);
            startForegroundService(intent2);
        }
    }
}