package me.daemon.screenrecorder

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.view.Surface
import me.daemon.storage.VideoMetadata
import me.daemon.storage.closeMedia
import me.daemon.storage.openFileDescriptor
import me.daemon.storage.openVideo

class ScreenRecorder(
    context: Context,
    width: Int,
    height: Int,
    densityDpi: Int,
) : ScreenAction(context.applicationContext, width, height, densityDpi) {

    private val mediaRecorder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context.applicationContext)
        } else {
            MediaRecorder()
        }
    }

    private var audioPlaybackCaptureConfiguration: AudioPlaybackCaptureConfiguration? = null

    private var videoUri: Uri? = null

    override fun surface(): Surface = mediaRecorder.surface

    override fun init() {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val uri = context.openVideo(
            VideoMetadata
                .builder()
                .name("screen-${timestamp()}.mp4")
                .width(width)
                .height(height)
                .build()
        ) ?: return

        videoUri = uri

        val descriptor = context.openFileDescriptor(uri) ?: return

        mediaRecorder.setOutputFile(descriptor.fileDescriptor)
        mediaRecorder.setVideoSize(width, height)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder.setVideoEncodingBitRate(2 * 1920 * 1080)
        mediaRecorder.setVideoFrameRate(18)
        mediaRecorder.prepare()
        mediaRecorder.start()
    }

    override fun start(intent: Intent) {
        super.start(intent)

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
        videoUri?.let {
            context.closeMedia(it)
            callback.onMediaSaved(it)
        }
        videoUri = null

        super.stop()
    }

}