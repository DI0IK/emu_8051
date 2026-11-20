package dev.dominikstahl.emu_8051.platform

private class JsBuzzerAudioPlayer : BuzzerAudioPlayer {
    private var lastTones: List<BuzzerTone> = emptyList()
    private var muted = false

    override fun setTones(tones: List<BuzzerTone>) {
        lastTones = tones
        sync()
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        sync()
    }

    override fun release() {
        lastTones = emptyList()
        jsReleaseBuzzer()
    }

    private fun sync() {
        val payload = if (muted || lastTones.isEmpty()) {
            ""
        } else {
            lastTones.filter { it.frequencyHz > 0.0 }
                .joinToString("\n") { "${it.id}=${it.frequencyHz}" }
        }
        jsUpdateBuzzer(payload)
    }
}

private fun jsUpdateBuzzer(payload: String) {
    js(
        """
        (function() {
            try {
                var b = globalThis.__emu8051Buzzer;
                if (!payload) {
                    if (b && b.voices) {
                        var now = b.ctx.currentTime;
                        var keys = Object.keys(b.voices);
                        for (var i = 0; i < keys.length; i++) {
                            b.voices[keys[i]].gain.gain.setTargetAtTime(0, now, 0.01);
                        }
                    }
                    return;
                }
                var Ctx = globalThis.AudioContext || globalThis.webkitAudioContext;
                if (!Ctx) return;
                if (!b) {
                    b = { ctx: new Ctx(), voices: {} };
                    globalThis.__emu8051Buzzer = b;
                }
                if (b.ctx.state === 'suspended') {
                    b.ctx.resume().catch(function(){});
                }
                var lines = payload.split('\n');
                var active = {};
                var count = 0;
                for (var i = 0; i < lines.length; i++) {
                    var line = lines[i];
                    var eq = line.lastIndexOf('=');
                    if (eq <= 0) continue;
                    var id = line.slice(0, eq);
                    var freq = parseFloat(line.slice(eq + 1));
                    if (!isFinite(freq) || freq <= 0) continue;
                    active[id] = freq;
                    count++;
                }
                var now = b.ctx.currentTime;
                var existingKeys = Object.keys(b.voices);
                for (var i = 0; i < existingKeys.length; i++) {
                    var k = existingKeys[i];
                    if (!(k in active)) {
                        b.voices[k].gain.gain.setTargetAtTime(0, now, 0.01);
                    }
                }
                if (count === 0) return;
                var targetGain = 0.05 / Math.sqrt(count);
                var activeKeys = Object.keys(active);
                for (var i = 0; i < activeKeys.length; i++) {
                    var id = activeKeys[i];
                    var v = b.voices[id];
                    if (!v) {
                        var osc = b.ctx.createOscillator();
                        var gain = b.ctx.createGain();
                        osc.type = 'square';
                        gain.gain.value = 0;
                        osc.connect(gain);
                        gain.connect(b.ctx.destination);
                        osc.start();
                        v = { osc: osc, gain: gain };
                        b.voices[id] = v;
                    }
                    var clamped = Math.max(40, Math.min(12000, active[id]));
                    v.osc.frequency.setTargetAtTime(clamped, now, 0.01);
                    v.gain.gain.setTargetAtTime(targetGain, now, 0.01);
                }
            } catch (e) {}
        })()
        """
    )
}

private fun jsReleaseBuzzer() {
    js(
        """
        (function() {
            try {
                var b = globalThis.__emu8051Buzzer;
                if (b) {
                    if (b.voices) {
                        var keys = Object.keys(b.voices);
                        for (var i = 0; i < keys.length; i++) {
                            try { b.voices[keys[i]].osc.stop(); } catch (e) {}
                        }
                    }
                    b.ctx.close();
                    globalThis.__emu8051Buzzer = null;
                }
            } catch (e) {}
        })()
        """
    )
}

actual fun createBuzzerAudioPlayer(): BuzzerAudioPlayer = JsBuzzerAudioPlayer()
