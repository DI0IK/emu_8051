@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.dominikstahl.emu_8051.platform

private class WasmJsBuzzerAudioPlayer : BuzzerAudioPlayer {
    private var lastFreq = 0.0
    private var lastActive = false
    private var muted = false

    override fun setTone(frequencyHz: Double, active: Boolean) {
        lastFreq = frequencyHz
        lastActive = active
        jsUpdateBuzzer(frequencyHz, active && !muted)
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        jsUpdateBuzzer(lastFreq, lastActive && !muted)
    }

    override fun release() {
        lastActive = false
        jsReleaseBuzzer()
    }
}

private fun jsUpdateBuzzer(freq: Double, active: Boolean): Unit = js(
    """(function() {
        try {
            var b = globalThis.__emu8051Buzzer;
            if (!active) {
                if (b && b.gain) {
                    b.gain.gain.setTargetAtTime(0, b.ctx.currentTime, 0.01);
                }
                return;
            }
            var Ctx = globalThis.AudioContext || globalThis.webkitAudioContext;
            if (!Ctx) return;
            if (!b) {
                var ctx = new Ctx();
                var osc = ctx.createOscillator();
                var gain = ctx.createGain();
                osc.type = 'square';
                gain.gain.value = 0;
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start();
                b = { ctx: ctx, osc: osc, gain: gain };
                globalThis.__emu8051Buzzer = b;
            }
            if (b.ctx.state === 'suspended') {
                b.ctx.resume().catch(function(){});
            }
            var clamped = Math.max(40, Math.min(12000, freq));
            b.osc.frequency.setTargetAtTime(clamped, b.ctx.currentTime, 0.01);
            b.gain.gain.setTargetAtTime(0.05, b.ctx.currentTime, 0.01);
        } catch (e) {}
    })()"""
)

private fun jsReleaseBuzzer(): Unit = js(
    """(function() {
        try {
            var b = globalThis.__emu8051Buzzer;
            if (b) {
                b.osc.stop();
                b.ctx.close();
                globalThis.__emu8051Buzzer = null;
            }
        } catch (e) {}
    })()"""
)

actual fun createBuzzerAudioPlayer(): BuzzerAudioPlayer = WasmJsBuzzerAudioPlayer()
