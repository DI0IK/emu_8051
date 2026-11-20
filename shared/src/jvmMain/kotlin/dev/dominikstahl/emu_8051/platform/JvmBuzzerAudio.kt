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
        var line: SourceDataLine? = null
        try {
            val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            if (!AudioSystem.isLineSupported(info)) return
            line = (AudioSystem.getLine(info) as SourceDataLine).apply {
                open(format, chunkSamples * 4)
                start()
            }
            val samples = ShortArray(chunkSamples)
            val buf = ByteArray(chunkSamples * 2)
            val mixer = BuzzerPcmMixer()
            var silentChunks = 0

            while (!closed && !Thread.currentThread().isInterrupted) {
                val tonesNow = if (isMuted) emptyList() else activeTones
                if (tonesNow.isEmpty()) {
                    mixer.reset()
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
                mixer.renderSamples(samples, tonesNow, sampleRate)
                for (i in 0 until chunkSamples) {
                    val sample = samples[i].toInt()
                    buf[i * 2] = (sample and 0xFF).toByte()
                    buf[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
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
