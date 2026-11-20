package dev.dominikstahl.emu_8051.platform

import kotlin.math.roundToInt
import kotlin.math.sqrt

data class BuzzerTone(val id: String, val frequencyHz: Double)

interface BuzzerAudioPlayer {
    fun setTones(tones: List<BuzzerTone>)
    fun setMuted(muted: Boolean)
    fun release()
}

/**
 * Polyphonic square-wave PCM mixer shared by sample-streaming backends (JVM, Android).
 * Tracks phase per buzzer ID so starting or stopping one buzzer does not glitch others.
 */
internal class BuzzerPcmMixer {
    private val phases = mutableMapOf<String, Double>()

    fun renderSamples(out: ShortArray, tones: List<BuzzerTone>, sampleRate: Int) {
        if (tones.isEmpty()) {
            out.fill(0)
            phases.clear()
            return
        }

        val activeIds = HashSet<String>(tones.size)
        for (t in tones) {
            if (t.frequencyHz > 0.0) activeIds.add(t.id)
        }
        phases.keys.retainAll(activeIds)

        if (activeIds.isEmpty()) {
            out.fill(0)
            return
        }

        val amp = 2200.0 / sqrt(activeIds.size.toDouble())
        val accum = DoubleArray(out.size)

        for (tone in tones) {
            if (tone.frequencyHz <= 0.0) continue
            val freq = tone.frequencyHz.coerceIn(40.0, 12000.0)
            val step = freq / sampleRate
            var phase = phases[tone.id] ?: 0.0
            for (i in out.indices) {
                phase += step
                if (phase >= 1.0) phase -= phase.toInt()
                accum[i] += if (phase < 0.5) amp else -amp
            }
            phases[tone.id] = phase
        }

        for (i in out.indices) {
            out[i] = accum[i].roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    fun reset() {
        phases.clear()
    }
}

expect fun createBuzzerAudioPlayer(): BuzzerAudioPlayer
