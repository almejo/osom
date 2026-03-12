package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class ImmediateALUSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	private void setupImmediate(int opcode, int immediate) {
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, opcode)
		mmu.setByte(0xC001, immediate)
	}

	// --- ADD A,n (0xC6) ---

	def "ADD A,n adds immediate to A"() {
		given: "A=0x10"
		cpu.AF.setHi(0x10)
		setupImmediate(0xC6, 0x05)

		when:
		cpu.execute()

		then: "A = 0x10 + 0x05 = 0x15"
		cpu.AF.getHi() == 0x15
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "ADD A,n sets flags on overflow"() {
		given: "A=0xFF"
		cpu.AF.setHi(0xFF)
		setupImmediate(0xC6, 0x01)

		when:
		cpu.execute()

		then: "A = 0x00, Z=1, H=1, C=1"
		cpu.AF.getHi() == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- ADC A,n (0xCE) ---

	def "ADC A,n adds immediate and carry to A"() {
		given: "A=0x10, carry=1"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		setupImmediate(0xCE, 0x05)

		when:
		cpu.execute()

		then: "A = 0x10 + 0x05 + 1 = 0x16"
		cpu.AF.getHi() == 0x16
	}

	// --- SUB n (0xD6) ---

	def "SUB n subtracts immediate from A"() {
		given: "A=0x10"
		cpu.AF.setHi(0x10)
		setupImmediate(0xD6, 0x05)

		when:
		cpu.execute()

		then: "A = 0x10 - 0x05 = 0x0B"
		cpu.AF.getHi() == 0x0B
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	def "SUB n sets carry on underflow"() {
		given: "A=0x00"
		cpu.AF.setHi(0x00)
		setupImmediate(0xD6, 0x01)

		when:
		cpu.execute()

		then: "A=0xFF, C=1"
		cpu.AF.getHi() == 0xFF
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- SBC A,n (0xDE) ---

	def "SBC A,n subtracts immediate and carry from A"() {
		given: "A=0x10, carry=1"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		setupImmediate(0xDE, 0x05)

		when:
		cpu.execute()

		then: "A = 0x10 - 0x05 - 1 = 0x0A"
		cpu.AF.getHi() == 0x0A
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	// --- XOR n (0xEE) ---

	def "XOR n computes A XOR immediate"() {
		given: "A=0xFF"
		cpu.AF.setHi(0xFF)
		setupImmediate(0xEE, 0x0F)

		when:
		cpu.execute()

		then: "A = 0xFF ^ 0x0F = 0xF0"
		cpu.AF.getHi() == 0xF0
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- OR n (0xF6) ---

	def "OR n computes A OR immediate"() {
		given: "A=0xF0"
		cpu.AF.setHi(0xF0)
		setupImmediate(0xF6, 0x0F)

		when:
		cpu.execute()

		then: "A = 0xF0 | 0x0F = 0xFF"
		cpu.AF.getHi() == 0xFF
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}
}
