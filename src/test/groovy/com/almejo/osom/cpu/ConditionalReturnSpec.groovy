package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class ConditionalReturnSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- RET NZ (0xC0) ---

	def "RET NZ pops stack and jumps when Z flag is clear"() {
		given: "PC at 0xC000, return address 0x1234 on stack, Z flag clear"
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34) // low byte
		mmu.setByte(0xDFFD, 0x12) // high byte
		mmu.setByte(0xC000, 0xC0) // RET NZ
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then: "PC = 0x1234, SP incremented by 2"
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFE

		and: "taken path: 20 T-cycles"
		cpu.getClockT() - initialT == 20
	}

	def "RET NZ does NOT pop stack when Z flag is set"() {
		given: "PC at 0xC000, Z flag set"
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34)
		mmu.setByte(0xDFFD, 0x12)
		mmu.setByte(0xC000, 0xC0) // RET NZ
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then: "PC advances by 1 (instruction length), SP unchanged"
		cpu.PC.getValue() == 0xC001
		cpu.SP.getValue() == 0xDFFC

		and: "not-taken path: 8 T-cycles"
		cpu.getClockT() - initialT == 8
	}

	// --- RET Z (0xC8) ---

	def "RET Z pops stack and jumps when Z flag is set"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34)
		mmu.setByte(0xDFFD, 0x12)
		mmu.setByte(0xC000, 0xC8) // RET Z
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFE
		cpu.getClockT() - initialT == 20
	}

	def "RET Z does NOT pop stack when Z flag is clear"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34)
		mmu.setByte(0xDFFD, 0x12)
		mmu.setByte(0xC000, 0xC8) // RET Z
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC001
		cpu.SP.getValue() == 0xDFFC
		cpu.getClockT() - initialT == 8
	}

	// --- RET NC (0xD0) ---

	def "RET NC pops stack and jumps when C flag is clear"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34)
		mmu.setByte(0xDFFD, 0x12)
		mmu.setByte(0xC000, 0xD0) // RET NC
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFE
		cpu.getClockT() - initialT == 20
	}

	def "RET NC does NOT pop stack when C flag is set"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34)
		mmu.setByte(0xDFFD, 0x12)
		mmu.setByte(0xC000, 0xD0) // RET NC
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC001
		cpu.SP.getValue() == 0xDFFC
		cpu.getClockT() - initialT == 8
	}

	// --- RET C (0xD8) ---

	def "RET C pops stack and jumps when C flag is set"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34)
		mmu.setByte(0xDFFD, 0x12)
		mmu.setByte(0xC000, 0xD8) // RET C
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFE
		cpu.getClockT() - initialT == 20
	}

	def "RET C does NOT pop stack when C flag is clear"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFC)
		mmu.setByte(0xDFFC, 0x34)
		mmu.setByte(0xDFFD, 0x12)
		mmu.setByte(0xC000, 0xD8) // RET C
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		int initialT = cpu.getClockT()

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC001
		cpu.SP.getValue() == 0xDFFC
		cpu.getClockT() - initialT == 8
	}
}
