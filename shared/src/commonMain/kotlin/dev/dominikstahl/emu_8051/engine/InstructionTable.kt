package dev.dominikstahl.emu_8051.engine

import dev.dominikstahl.emu_8051.engine.ops.InstructionHandler

class InstructionTable(private val state: CpuState) {
    private val illegal: (Int) -> Int = { state.pc++; 1 }
    private val entries = Array(256) { illegal }

    fun register(handler: InstructionHandler) {
        for (inst in handler.handledInstructions) {
            for (rawOpcode in inst.pattern.opcodes()) {
                entries[rawOpcode] = { handler.handle(inst, rawOpcode) }
            }
        }
    }

    fun build(): Array<(Int) -> Int> = entries
}
