package me.daemon.screenrecorder.demo

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


class ScreenRecordService : Service() {

    companion object {
        private const val TAG = "ScreenRecordService"
        private const val NOTIFICATION_CHANNEL_ID = "ScreenRecordService_nofity"
        private const val NOTIFICATION_CHANNEL_NAME = "ScreenRecordService"
        private const val NOTIFICATION_CHANNEL_DESC = "ScreenRecordService"
        private const val NOTIFICATION_ID = 1000
        private const val NOTIFICATION_TICKER = "RecorderApp"
    }

    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val tempDisplayMetrics = DisplayMetrics()

    private val screenShot by lazy {
        windowManager.defaultDisplay.getRealMetrics(tempDisplayMetrics)
        ScreenShot(
            this,
            tempDisplayMetrics.widthPixels,
            tempDisplayMetrics.heightPixels,
            tempDisplayMetrics.densityDpi,
        )
    }

    private val screenRecorder by lazy {
        windowManager.defaultDisplay.getRealMetrics(tempDisplayMetrics)
        ScreenRecorder(
            this,
            tempDisplayMetrics.widthPixels,
            tempDisplayMetrics.heightPixels,
            tempDisplayMetrics.densityDpi,
        )
    }

    private var screenAction = screenShot

    private var resultCode: Int = -1
    private var resultData: Intent? = null

    private var running = false

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()
        createNotification()

    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        resultData = intent?.getParcelableExtra("data")
        createNotification()

        screenAction.init(resultCode, resultData!!)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        screenAction.destroy()

        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNotification() {
        Log.i(TAG, "notification: " + Build.VERSION.SDK_INT)
        val notificationIntent = Intent(this, ScreenRecordService::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)
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