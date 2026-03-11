package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class InterruptHandlingSpec extends Specification {

	MMU mmu
	Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	def "checkInterrupts serves highest-priority interrupt first when multiple pending"() {
		given: "IME is enabled"
		cpu.setInterruptionsEnabled(true)

		and: "both V-Blank (bit 0) and Timer (bit 2) are requested and enabled"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x05)
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x05)

		and: "PC is at a known address and SP is set"
		cpu.PC.setValue(0x1234)
		cpu.SP.setValue(0xFFFE)

		when: "checking interrupts"
		cpu.checkInterrupts()

		then: "V-Blank (highest priority) is served: PC jumps to V-Blank handler"
		cpu.PC.getValue() == Z80Cpu.INTERRUPT_ADDRESS_V_BLANK

		and: "Timer IF bit 2 is still set (only one interrupt served per call)"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x04) == 0x04
	}

	def "serveInterrupt pushes PC to stack, jumps to handler, disables IME, and clears IF bit"() {
		given: "IME is enabled"
		cpu.setInterruptionsEnabled(true)

		and: "V-Blank interrupt is requested and enabled"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x01)
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x01)

		and: "PC is at a known address"
		cpu.PC.setValue(0xABCD)
		cpu.SP.setValue(0xFFFE)

		when: "checking interrupts"
		cpu.checkInterrupts()

		then: "PC pushed to stack (SP decremented by 2)"
		cpu.SP.getValue() == 0xFFFC

		and: "the return address is stored on the stack"
		mmu.getWord(0xFFFC) == 0xABCD

		and: "PC jumps to V-Blank handler address"
		cpu.PC.getValue() == Z80Cpu.INTERRUPT_ADDRESS_V_BLANK

		and: "IF bit 0 is cleared"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x00

		and: "IME is disabled"
		!cpu.@interruptionsEnabled
	}

	def "EI enables interrupts with one-instruction delay via actual opcode execution"() {
		given: "interrupts are disabled"
		cpu.setInterruptionsEnabled(false)

		and: "EI opcode (0xFB) is at 0xC000 followed by NOP (0x00) at 0xC001"
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xFB)
		mmu.setByte(0xC001, 0x00)

		when: "EI opcode is executed"
		cpu.execute()

		then: "interrupts are NOT yet enabled (delayed by one instruction)"
		!cpu.@interruptionsEnabled

		and: "pendingInterruptEnable is set"
		cpu.@pendingInterruptEnable

		when: "the next instruction (NOP) is executed"
		cpu.execute()

		then: "interrupts are now enabled (pending was applied before the NOP)"
		cpu.@interruptionsEnabled

		and: "pendingInterruptEnable is cleared"
		!cpu.@pendingInterruptEnable
	}

	def "DI disables interrupts immediately via actual opcode execution"() {
		given: "interrupts are enabled and DI opcode (0xF3) is at 0xC000"
		cpu.setInterruptionsEnabled(true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xF3)

		when: "DI opcode is executed"
		cpu.execute()

		then: "interrupts are disabled immediately"
		!cpu.@interruptionsEnabled
	}

	def "RETI enables interrupts immediately and pops PC from stack via actual opcode execution"() {
		given: "interrupts are disabled, a return address is on the stack, and RETI opcode (0xD9) is at 0xC000"
		cpu.setInterruptionsEnabled(false)
		cpu.SP.setValue(0xFFFC)
		mmu.setWord(0xFFFC, 0x1234)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xD9)

		when: "RETI opcode is executed"
		cpu.execute()

		then: "PC is restored from stack"
		cpu.PC.getValue() == 0x1234

		and: "SP is incremented by 2"
		cpu.SP.getValue() == 0xFFFE

		and: "interrupts are enabled immediately (no delay like EI)"
		cpu.@interruptionsEnabled
	}

	def "interrupts do not fire when IME is false"() {
		given: "IME is disabled"
		cpu.setInterruptionsEnabled(false)

		and: "V-Blank interrupt is requested and enabled"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x01)
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x01)

		and: "PC is at a known address"
		cpu.PC.setValue(0x1234)
		cpu.SP.setValue(0xFFFE)

		when: "checking interrupts"
		cpu.checkInterrupts()

		then: "PC is unchanged (no interrupt served)"
		cpu.PC.getValue() == 0x1234

		and: "SP is unchanged"
		cpu.SP.getValue() == 0xFFFE

		and: "IF bit is still set (not cleared)"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x01
	}

	def "timer interrupt dispatches to correct handler address"() {
		given: "IME is enabled"
		cpu.setInterruptionsEnabled(true)

		and: "Timer interrupt is requested and enabled"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x04)
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x04)

		and: "PC and SP are set"
		cpu.PC.setValue(0x5678)
		cpu.SP.setValue(0xFFFE)

		when: "checking interrupts"
		cpu.checkInterrupts()

		then: "PC jumps to timer handler"
		cpu.PC.getValue() == Z80Cpu.INTERRUPT_ADDRESS_TIMER
	}

	def "serial interrupt dispatches to correct handler address"() {
		given: "IME is enabled"
		cpu.setInterruptionsEnabled(true)

		and: "Serial interrupt is requested and enabled"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x08)
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x08)

		and: "PC and SP are set"
		cpu.PC.setValue(0x5678)
		cpu.SP.setValue(0xFFFE)

		when: "checking interrupts"
		cpu.checkInterrupts()

		then: "PC jumps to serial handler"
		cpu.PC.getValue() == Z80Cpu.INTERRUPT_ADDRESS_SERIAL
	}

	def "INTERRUPT_ADDRESSES map contains all five handler addresses"() {
		expect: "all five interrupt types have correct handler addresses"
		Z80Cpu.INTERRUPT_ADDRESS_V_BLANK == 0x40
		Z80Cpu.INTERRUPT_ADDRESS_LCD == 0x48
		Z80Cpu.INTERRUPT_ADDRESS_TIMER == 0x50
		Z80Cpu.INTERRUPT_ADDRESS_SERIAL == 0x58
		Z80Cpu.INTERRUPT_ADDRESS_JOY_PAD == 0x60
	}
}
