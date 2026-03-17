package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class HALTSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	def "HALT instruction sets halted flag"() {
		given: "PC at 0xC000 with HALT opcode"
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x76) // HALT

		when:
		cpu.execute()

		then: "CPU is halted"
		cpu.halted
	}

	def "halted CPU consumes 4 T-cycles without advancing PC"() {
		given: "CPU is halted at 0xC000"
		cpu.PC.setValue(0xC000)
		cpu.halted = true
		int initialT = cpu.getClockT()
		int initialPC = cpu.PC.getValue()

		when: "execute is called"
		cpu.execute()

		then: "4 T-cycles consumed, PC unchanged"
		cpu.getClockT() - initialT == 4
		cpu.PC.getValue() == initialPC
	}

	def "halted CPU un-halts when enabled interrupt is pending"() {
		given: "CPU is halted, V-Blank interrupt requested and enabled"
		cpu.halted = true
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x01) // V-Blank requested
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x01)    // V-Blank enabled

		when: "checkInterrupts is called"
		cpu.checkInterrupts()

		then: "CPU is no longer halted"
		!cpu.halted
	}

	def "halted CPU does NOT un-halt when no enabled interrupt is pending"() {
		given: "CPU is halted, V-Blank requested but NOT enabled"
		cpu.halted = true
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x01) // V-Blank requested
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x00)    // no interrupts enabled

		when: "checkInterrupts is called"
		cpu.checkInterrupts()

		then: "CPU remains halted"
		cpu.halted
	}

	def "halted CPU does NOT un-halt when interrupt enabled but not requested"() {
		given: "CPU is halted, V-Blank enabled but not requested"
		cpu.halted = true
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x00) // nothing requested
		mmu.setByte(MMU.INTERRUPT_ENABLED_ADDRESS, 0x01)    // V-Blank enabled

		when:
		cpu.checkInterrupts()

		then:
		cpu.halted
	}
}
