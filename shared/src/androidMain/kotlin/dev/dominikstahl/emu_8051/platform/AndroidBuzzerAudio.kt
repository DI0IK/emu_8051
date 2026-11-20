package dev.dominikstahl.emu_8051.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.Volatile
import kotlin.concurrent.thread

private class AndroidBuzzerAudioPlayer : BuzzerAudioPlayer {
    private val sampleRate = 22050
    private val chunkSamples = 330

    @Volatile private var activeTones: List<BuzzerTone> = emptyList()
    @Volatile private var isMuted = false
    @Volatile private var closed = false

    private var workerThread: Thread? = null
    private val lock = Any()

    override fun setTones(tones: List<BuzzerTone>) {
        if (closed) return
        activeTones = tones
        if (tones.isNotEmpty() && !isMuted && workerThread == null) {
            synchronized(lock) {
                if (workerThread == null && !closed) {
                    workerThread = thread(isDaemon = true, name = "emu8051-buzzer") {
                        audioLoop()
                    }
                }
            }
        }
    }

    override fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    override fun release() {
        closed = true
        activeTones = emptyList()
        workerThread?.interrupt()
        workerThread = null
    }

    private fun audioLoop() {
        var track: AudioTrack? = null
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuf <= 0) return
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, chunkSamples * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()

            val buf = ShortArray(chunkSamples)
            val mixer = BuzzerPcmMixer()
            var silentChunks = 0

            while (!closed && !Thread.currentThread().isInterrupted) {
                val tonesNow = if (isMuted) emptyList() else activeTones
                if (tonesNow.isEmpty()) {
                    mixer.reset()
                    buf.fill(0)
                    track.write(buf, 0, buf.size)
                    silentChunks++
                    if (silentChunks > 40) break
                    continue
                }
                silentChunks = 0
                mixer.renderSamples(buf, tonesNow, sampleRate)
                track.write(buf, 0, buf.size)
            }
        } catch (_: Throwable) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {}
            synchronized(lock) {
                workerThread = null
            }
        }
    }
}

actual fun createBuzzerAudioPlayer(): BuzzerAudioPlayer = AndroidBuzzerAudioPlayer()
