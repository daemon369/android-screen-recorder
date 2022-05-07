package me.daemon.screenrecorder.demo

import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface

class ScreenShot(
    context: Context,
    width: Int,
    height: Int,
    densityDpi: Int,
) : ScreenAction(context, width, height, densityDpi) {

    private val imageReader by lazy {
        ImageReader.newInstance(
            width,
            height,
            ImageFormat.JPEG,
            10
        ).apply {
            setOnImageAvailableListener(
                {
                    it ?: return@setOnImageAvailableListener
                    val cur = System.currentTimeMillis()
                    if (time != 0L) {
                        Log.e("ScreenShot", "onImageAvailable interval=${cur - time}")
                    }
                    time = cur
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

    override fun start(resultCode: Int, intent: Intent) {
        super.start(resultCode, intent)
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