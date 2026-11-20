package dev.dominikstahl.emu_8051.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.Volatile
import kotlin.concurrent.thread

private class AndroidBuzzerAudioPlayer : BuzzerAudioPlayer {
    private val sampleRate = 22050
    private val chunkSamples = 330

    @Volatile private var targetFreq = 0.0
    @Volatile private var isActive = false
    @Volatile private var isMuted = false
    @Volatile private var closed = false

    private var workerThread: Thread? = null
    private val lock = Any()

    override fun setTone(frequencyHz: Double, active: Boolean) {
        if (closed) return
        targetFreq = frequencyHz
        isActive = active
        if (active && !isMuted && workerThread == null) {
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
        isActive = false
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
            var phase = 0.0
            var silentChunks = 0

            while (!closed && !Thread.currentThread().isInterrupted) {
                val activeNow = isActive && !isMuted && targetFreq > 0.0
                if (!activeNow) {
                    buf.fill(0)
                    track.write(buf, 0, buf.size)
                    silentChunks++
                    if (silentChunks > 40) break
                    continue
                }
                silentChunks = 0
                val freq = targetFreq.coerceIn(40.0, 12000.0)
                val step = freq / sampleRate
                for (i in 0 until chunkSamples) {
                    phase += step
                    if (phase >= 1.0) phase -= phase.toInt()
                    buf[i] = if (phase < 0.5) 2200 else -2200
                }
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
