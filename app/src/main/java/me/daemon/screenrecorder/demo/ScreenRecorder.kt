package me.daemon.screenrecorder.demo

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaRecorder
import android.os.Build
import android.view.Surface
import java.io.File

class ScreenRecorder(
    context: Context,
    width: Int,
    height: Int,
    densityDpi: Int,
) : ScreenAction(context, width, height, densityDpi) {

    private val mediaRecorder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    private var audioPlaybackCaptureConfiguration: AudioPlaybackCaptureConfiguration? = null

    override fun surface(): Surface = mediaRecorder.surface

    override fun init() {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        val f = File(context.getExternalFilesDir(null), "video.mp4")
        f.parentFile?.mkdirs()
        mediaRecorder.setOutputFile(f.absolutePath)
        mediaRecorder.setVideoSize(width, height)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder.setVideoEncodingBitRate(2 * 1920 * 1080)
        mediaRecorder.setVideoFrameRate(18)
        mediaRecorder.prepare()
        mediaRecorder.start()
    }

    override fun start(resultCode: Int, intent: Intent) {
        super.start(resultCode, intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            builder.addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            builder.addMatchingUsage(AudioAttributes.USAGE_ALARM)
            builder.addMatchingUsage(AudioAttributes.USAGE_GAME)
            audioPlaybackCaptureConfiguration = builder.build()
        }
    }

    override fun stop() {
        mediaRecorder.stop()
        mediaRecorder.release()

        super.stop()
    }

}