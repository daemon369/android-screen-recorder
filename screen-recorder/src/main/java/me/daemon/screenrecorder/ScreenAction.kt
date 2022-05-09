package me.daemon.screenrecorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.view.Surface
import me.daemon.logger.logger
import me.daemon.view.common.screenHeight
import me.daemon.view.common.screenWidth

abstract class ScreenAction(
    val context: Context,
    val width: Int,
    val height: Int,
    val densityDpi: Int,
) {

    protected val log by logger()

    protected val mediaProjectionManager by lazy {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    protected var mediaProjection: MediaProjection? = null
    protected var virtualDisplay: VirtualDisplay? = null

    abstract fun surface(): Surface

    open fun init() = Unit

    open fun start(intent: Intent) {
        log.i("start")
        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            javaClass.name,
            screenWidth,
            screenHeight,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface(),
            null,
            null
        )
    }

    open fun stop() {
        log.i("stop")
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.apply {
            stop()
        }
        mediaProjection = null
    }

}