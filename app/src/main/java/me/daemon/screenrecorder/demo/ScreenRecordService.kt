package me.daemon.screenrecorder.demo

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import me.daemon.logger.logger


class ScreenRecordService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ScreenRecordService_nofity"
        private const val NOTIFICATION_CHANNEL_NAME = "ScreenRecordService"
        private const val NOTIFICATION_CHANNEL_DESC = "ScreenRecordService"
        private const val NOTIFICATION_ID = 1000
        private const val NOTIFICATION_TICKER = "RecorderApp"
    }

    private val log by logger()

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

    private lateinit var screenAction: ScreenAction

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()
        createNotification()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getIntExtra("command", -1) ?: -1
        if (command == -1) {
            return super.onStartCommand(intent, flags, startId)
        }

        when (command) {
            1 -> {
                // start recording screen
                screenAction = screenRecorder
                val resultData: Intent? = intent?.getParcelableExtra("data")
                if (resultData != null) {
                    screenAction.init()
                    createNotification()
                    screenAction.start(resultData)
                }
            }
            2 -> {
                // start screen capture
                screenAction = screenShot
                val resultData: Intent? = intent?.getParcelableExtra("data")
                if (resultData != null) {
                    screenAction.init()
                    createNotification()
                    screenAction.start(resultData)
                }
            }
            3 -> {
                // stop action
                if (::screenAction.isInitialized) {
                    screenAction.stop()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        screenAction.stop()

        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNotification() {
        log.i("notification: " + Build.VERSION.SDK_INT)
        val notificationIntent = Intent(this, ScreenRecordService::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
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