package dev.dominikstahl.emu_8051.platform

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.concurrent.Volatile

private class JvmBuzzerAudioPlayer : BuzzerAudioPlayer {
    private val sampleRate = 22050
    private val chunkSamples = 330 // ~15 ms

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
        var line: SourceDataLine? = null
        try {
            val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) return
            line = (AudioSystem.getLine(info) as SourceDataLine).apply {
                open(format, chunkSamples * 4)
                start()
            }
            val buf = ByteArray(chunkSamples * 2)
            var phase = 0.0
            var silentChunks = 0

            while (!closed && !Thread.currentThread().isInterrupted) {
                val activeNow = isActive && !isMuted && targetFreq > 0.0
                if (!activeNow) {
                    buf.fill(0)
                    line.write(buf, 0, buf.size)
                    silentChunks++
                    if (silentChunks > 40) {
                        // Release audio device after ~600ms of silence; reopen on next tone
                        break
                    }
                    continue
                }
                silentChunks = 0
                val freq = targetFreq.coerceIn(40.0, 12000.0)
                val step = freq / sampleRate
                for (i in 0 until chunkSamples) {
                    phase += step
                    if (phase >= 1.0) phase -= phase.toInt()
                    val sample: Short = if (phase < 0.5) 2200 else -2200
                    buf[i * 2] = (sample.toInt() and 0xFF).toByte()
                    buf[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }
                line.write(buf, 0, buf.size)
            }
        } catch (_: Throwable) {
            // Audio device unavailable (e.g., headless CI) — silently degrade to visual only
        } finally {
            try {
                line?.stop()
                line?.close()
            } catch (_: Throwable) {}
            synchronized(lock) {
                workerThread = null
            }
        }
    }
}

actual fun createBuzzerAudioPlayer(): BuzzerAudioPlayer = JvmBuzzerAudioPlayer()
