package dev.dominikstahl.emu_8051.platform

private object IosBuzzerAudioPlayer : BuzzerAudioPlayer {
    override fun setTones(tones: List<BuzzerTone>) {}
    override fun setMuted(muted: Boolean) {}
    override fun release() {}
}

actual fun createBuzzerAudioPlayer(): BuzzerAudioPlayer = IosBuzzerAudioPlayer
