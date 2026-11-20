package dev.dominikstahl.emu_8051.platform

private object IosBuzzerAudioPlayer : BuzzerAudioPlayer {
    override fun setTone(frequencyHz: Double, active: Boolean) {}
    override fun setMuted(muted: Boolean) {}
    override fun release() {}
}

actual fun createBuzzerAudioPlayer(): BuzzerAudioPlayer = IosBuzzerAudioPlayer
