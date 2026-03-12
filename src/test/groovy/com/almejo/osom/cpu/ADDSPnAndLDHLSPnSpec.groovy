package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class ADDSPnAndLDHLSPnSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- ADD SP,n (0xE8) ---

	def "ADD SP,n with positive offset"() {
		given: "SP=0xFFF0, n=0x05"
		cpu.SP.setValue(0xFFF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xE8)
		mmu.setByte(0xC001, 0x05) // +5

		when:
		cpu.execute()

		then: "SP=0xFFF5, Z=0, N=0"
		cpu.SP.getValue() == 0xFFF5
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	def "ADD SP,n with negative offset (signed byte)"() {
		given: "SP=0xFFF0, n=0xFB (-5 signed)"
		cpu.SP.setValue(0xFFF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xE8)
		mmu.setByte(0xC001, 0xFB) // -5

		when:
		cpu.execute()

		then: "SP=0xFFEB"
		cpu.SP.getValue() == 0xFFEB
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	def "ADD SP,n sets half-carry and carry flags correctly"() {
		given: "SP=0x00FF, n=0x01 — carry from bit 7, half-carry from bit 3"
		cpu.SP.setValue(0x00FF)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xE8)
		mmu.setByte(0xC001, 0x01) // +1

		when:
		cpu.execute()

		then: "SP=0x0100, H=1, C=1"
		cpu.SP.getValue() == 0x0100
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "ADD SP,n always clears zero flag"() {
		given: "Z flag set before instruction"
		cpu.SP.setValue(0x0000)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xE8)
		mmu.setByte(0xC001, 0x00)

		when:
		cpu.execute()

		then: "Z=0 always"
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	// --- LD HL,SP+n (0xF8) ---

	def "LD HL,SP+n with positive offset"() {
		given: "SP=0xFFF0, n=0x05"
		cpu.SP.setValue(0xFFF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xF8)
		mmu.setByte(0xC001, 0x05)

		when:
		cpu.execute()

		then: "HL=0xFFF5, SP unchanged"
		cpu.HL.getValue() == 0xFFF5
		cpu.SP.getValue() == 0xFFF0
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	def "LD HL,SP+n with negative offset"() {
		given: "SP=0xFFF0, n=0xFB (-5)"
		cpu.SP.setValue(0xFFF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xF8)
		mmu.setByte(0xC001, 0xFB)

		when:
		cpu.execute()

		then: "HL=0xFFEB"
		cpu.HL.getValue() == 0xFFEB
	}

	def "LD HL,SP+n sets carry flags same as ADD SP,n"() {
		given: "SP=0x00FF, n=0x01"
		cpu.SP.setValue(0x00FF)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xF8)
		mmu.setByte(0xC001, 0x01)

		when:
		cpu.execute()

		then: "HL=0x0100, H=1, C=1"
		cpu.HL.getValue() == 0x0100
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}
}
