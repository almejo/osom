package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class MemoryALUSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	private void setupMemoryOp(int opcode, int hlAddress, int memValue) {
		cpu.HL.setValue(hlAddress)
		mmu.setByte(hlAddress, memValue)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, opcode)
	}

	// --- ADC A,(HL) (0x8E) ---

	def "ADC A,(HL) adds memory value and carry to A"() {
		given: "A=0x10, (HL)=0x05, carry=1"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		setupMemoryOp(0x8E, 0xD000, 0x05)

		when:
		cpu.execute()

		then: "A = 0x10 + 0x05 + 1 = 0x16"
		cpu.AF.getHi() == 0x16
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	// --- SUB (HL) (0x96) ---

	def "SUB (HL) subtracts memory value from A"() {
		given: "A=0x10, (HL)=0x05"
		cpu.AF.setHi(0x10)
		setupMemoryOp(0x96, 0xD000, 0x05)

		when:
		cpu.execute()

		then: "A = 0x10 - 0x05 = 0x0B"
		cpu.AF.getHi() == 0x0B
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- SBC A,(HL) (0x9E) ---

	def "SBC A,(HL) subtracts memory value and carry from A"() {
		given: "A=0x10, (HL)=0x05, carry=1"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		setupMemoryOp(0x9E, 0xD000, 0x05)

		when:
		cpu.execute()

		then: "A = 0x10 - 0x05 - 1 = 0x0A"
		cpu.AF.getHi() == 0x0A
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	// --- AND (HL) (0xA6) ---

	def "AND (HL) computes A AND memory value"() {
		given: "A=0xFF, (HL)=0x0F"
		cpu.AF.setHi(0xFF)
		setupMemoryOp(0xA6, 0xD000, 0x0F)

		when:
		cpu.execute()

		then: "A = 0xFF & 0x0F = 0x0F, H=1"
		cpu.AF.getHi() == 0x0F
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	// --- XOR (HL) (0xAE) ---

	def "XOR (HL) computes A XOR memory value"() {
		given: "A=0xFF, (HL)=0xFF"
		cpu.AF.setHi(0xFF)
		setupMemoryOp(0xAE, 0xD000, 0xFF)

		when:
		cpu.execute()

		then: "A = 0xFF ^ 0xFF = 0x00, Z=1"
		cpu.AF.getHi() == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- OR (HL) (0xB6) ---

	def "OR (HL) computes A OR memory value"() {
		given: "A=0xF0, (HL)=0x0F"
		cpu.AF.setHi(0xF0)
		setupMemoryOp(0xB6, 0xD000, 0x0F)

		when:
		cpu.execute()

		then: "A = 0xF0 | 0x0F = 0xFF"
		cpu.AF.getHi() == 0xFF
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}
}
