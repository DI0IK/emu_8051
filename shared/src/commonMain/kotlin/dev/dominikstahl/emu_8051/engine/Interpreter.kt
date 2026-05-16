package dev.dominikstahl.emu_8051.engine

import dev.dominikstahl.emu_8051.engine.ops.*

@OptIn(ExperimentalUnsignedTypes::class)
class Interpreter(val state: CpuState) {

    val interruptController = InterruptController(state).also { state.interruptController = it }
    val timerController = TimerController(state).also { state.timerController = it }

    private val instructionTable: Array<(Int) -> Int> = InstructionTable(state).apply {
        register(ADDCHandler(state))
        register(AJMPHandler(state))
        register(ANLHandler(state))
        register(BitHandler(state))
        register(CJNEHandler(state))
        register(DAHandler(state))
        register(DECHandler(state))
        register(DIVHandler(state))
        register(DJNZHandler(state))
        register(INCHandler(state))
        register(JumpHandler(state))
        register(MOVHandler(state))
        register(MULHandler(state))
        register(NOPHandler(state))
        register(ORLHandler(state))
        register(RotateHandler(state))
        register(SJMPHandler(state))
        register(SUBBHandler(state))
        register(SWAPHandler(state))
        register(StackHandler(state))
        register(XCHHandler(state))
        register(XRLHandler(state))
    }.build()

    fun step(): Int {
        val opcode = state.rom[state.pc].toInt()

        state.interruptControllerAccessFlag = false
        interruptController.lastInterruptCycles = 0

        val cycles = instructionTable[opcode](opcode)
        state.totalCycles += cycles

        repeat(cycles) { timerController.tick() }

        interruptController.sample()

        val isBlocking = Instruction.byOpcode[opcode] == Instruction.RETI || state.interruptControllerAccessFlag
        if (!isBlocking) {
            interruptController.poll()
        }
        state.totalCycles += interruptController.lastInterruptCycles

        return cycles + interruptController.lastInterruptCycles
    }
}
