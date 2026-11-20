package dev.dominikstahl.emu_8051.platform

interface BuzzerAudioPlayer {
    fun setTone(frequencyHz: Double, active: Boolean)
    fun setMuted(muted: Boolean)
    fun release()
}

expect fun createBuzzerAudioPlayer(): BuzzerAudioPlayer
