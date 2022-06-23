package me.daemon.screenrecorder

import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface

class ScreenShot(
    context: Context,
    width: Int,
    height: Int,
    densityDpi: Int,
) : ScreenAction(context.applicationContext, width, height, densityDpi) {

    private val imageReader by lazy {
        ImageReader.newInstance(
            width,
            height,
            ImageFormat.JPEG,
//            PixelFormat.RGBA_8888,
            10
        ).apply {
            setOnImageAvailableListener(
                { reader ->
                    reader ?: return@setOnImageAvailableListener
                    val cur = System.currentTimeMillis()
                    if (time != 0L) {
                        log.e("onImageAvailable interval=", cur - time)
                    } else {
                        log.e("onImageAvailable")
                    }
                    time = cur

                    reader.acquireNextImage().use { image ->
                        imageCallback(image)
                    }
                },
                getOrCreateHandler(),
            )
        }
    }

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var time: Long = 0

    override fun surface(): Surface = imageReader.surface

    override fun init() = Unit

    override fun start(intent: Intent) {
        super.start(intent)
    }

    override fun stop() {
        destroyHandler()
        super.stop()
    }

    private fun getOrCreateHandler(): Handler {
        val h = handler
        if (h != null) return h

        HandlerThread("ScreenShot-ImageReader-Thread").apply {
            handlerThread = this
            start()
            Handler(this.looper).apply {
                handler = this
                return this
            }
        }
    }

    private fun destroyHandler() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        handlerThread?.quitSafely()
        handlerThread = null
    }

}