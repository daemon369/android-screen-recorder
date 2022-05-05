package me.daemon.screenrecorder.demo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File


class MainActivity : AppCompatActivity() {

    private val mediaRecord by lazy { MediaRecorder() }
    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var started = false

    private val callback = object : MediaProjection.Callback() {
        override fun onStop() {
            mediaRecord.stop()
            mediaRecord.release()
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection?.apply {
                stop()
            }
            mediaProjection = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    2
                )
                return@setOnClickListener
            }

            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)

            if (!started) {
                mediaRecord.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecord.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                mediaRecord.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaRecord.setOutputFile(
                        File(getExternalFilesDir(null), "video.mp4")
                    )
                }
                mediaRecord.setVideoSize(size.x, size.y)
                mediaRecord.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                mediaRecord.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                mediaRecord.setVideoEncodingBitRate(512 * 1000)
                mediaRecord.setVideoFrameRate(30)
                val rotation = windowManager.defaultDisplay.rotation
//                val orientation: Int = ORIENTATIONS.get(rotation + 90)
//                mediaRecord.setOrientationHint(orientation)
                mediaRecord.prepare()

                if (mediaProjection == null) {
                    startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 1);
                }
            } else {
                mediaRecord.stop()
                mediaRecord.release()
                virtualDisplay?.release()
                virtualDisplay = null
                mediaProjection?.apply {
                    stop()
                }
                mediaProjection = null
            }

            started = !started
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (1 == requestCode && Activity.RESULT_OK == resultCode) {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!).apply {
                registerCallback(callback, null)
                virtualDisplay = createVirtualDisplay(
                    "MainActivity",
                    displayMetrics.widthPixels,
                    displayMetrics.heightPixels,
                    displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecord.surface,
                    null,
                    null
                );
            }

            mediaRecord.start()
        }
    }
}