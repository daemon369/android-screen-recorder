package me.daemon.screenrecorder.demo

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import me.daemon.view.common.screenHeight
import me.daemon.view.common.screenWidth
import java.io.File


class ScreenRecordService : Service() {

    companion object {
        private const val TAG = "ScreenRecordService"
        private const val NOTIFICATION_CHANNEL_ID = "ScreenRecordService_nofity"
        private const val NOTIFICATION_CHANNEL_NAME = "ScreenRecordService"
        private const val NOTIFICATION_CHANNEL_DESC = "ScreenRecordService"
        private const val NOTIFICATION_ID = 1000
        private const val NOTIFICATION_TICKER = "RecorderApp"
    }

    private val mediaProjectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var mediaProjection: MediaProjection? = null
    private var audioPlaybackCaptureConfiguration: AudioPlaybackCaptureConfiguration? = null

    private var virtualDisplay: VirtualDisplay? = null
    private val mediaRecorder = MediaRecorder()

    private var resultCode: Int = -1
    private var resultData: Intent? = null

    private var running = false

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()
        createNotification()

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        val f = File(getExternalFilesDir(null), "video.mp4")
        mediaRecorder.setOutputFile(f.absolutePath)
        mediaRecorder.setVideoSize(screenWidth, screenHeight)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder.setVideoEncodingBitRate(2 * 1920 * 1080)
        mediaRecorder.setVideoFrameRate(18)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        resultData = intent?.getParcelableExtra("data")
        Log.i(TAG, "onStartCommand: $resultCode")
        Log.i(TAG, "onStartCommand: $resultData")
        createNotification()

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData!!)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecordService",
            screenWidth,
            screenHeight,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.surface,
            null,
            null
        )

        mediaRecorder.start()

        Log.i(TAG, "onStartCommand: $mediaProjection")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            builder.addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            builder.addMatchingUsage(AudioAttributes.USAGE_ALARM)
            builder.addMatchingUsage(AudioAttributes.USAGE_GAME)
            audioPlaybackCaptureConfiguration = builder.build()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        running = false;
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNotification() {
        Log.i(TAG, "notification: " + Build.VERSION.SDK_INT)
        val notificationIntent = Intent(this, ScreenRecordService::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_foreground
                    )
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Screen recorder service")
                .setContentText("Screen recorder service")
                .setTicker(NOTIFICATION_TICKER)
                .setContentIntent(pendingIntent)
        val notification: Notification = notificationBuilder.build()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = NOTIFICATION_CHANNEL_DESC
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

}