package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class ParameterizedOpsSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- ADC_r: ADC A,B (0x88) ---

	def "ADC A,B adds B and carry to A with correct flags"() {
		given: "A=0x10, B=0x05, carry=1"
		cpu.AF.setHi(0x10)
		cpu.BC.setHi(0x05)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x88) // ADC A,B

		when:
		cpu.execute()

		then: "A = 0x10 + 0x05 + 1 = 0x16"
		cpu.AF.getHi() == 0x16
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	// --- SBC_r: SBC A,B (0x98) ---

	def "SBC A,B subtracts B and carry from A with correct flags"() {
		given: "A=0x10, B=0x05, carry=1"
		cpu.AF.setHi(0x10)
		cpu.BC.setHi(0x05)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x98) // SBC A,B

		when:
		cpu.execute()

		then: "A = 0x10 - 0x05 - 1 = 0x0A"
		cpu.AF.getHi() == 0x0A
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	// --- XOR_r: XOR B (0xA8) ---

	def "XOR B computes A XOR B with correct flags"() {
		given: "A=0xFF, B=0x0F"
		cpu.AF.setHi(0xFF)
		cpu.BC.setHi(0x0F)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xA8) // XOR B

		when:
		cpu.execute()

		then: "A = 0xFF ^ 0x0F = 0xF0"
		cpu.AF.getHi() == 0xF0
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- CP_r: CP B (0xB8) ---

	def "CP B compares A with B, sets flags without modifying A"() {
		given: "A=0x10, B=0x10"
		cpu.AF.setHi(0x10)
		cpu.BC.setHi(0x10)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xB8) // CP B

		when:
		cpu.execute()

		then: "A unchanged, Z=1, N=1"
		cpu.AF.getHi() == 0x10
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- LD_aHL_r: LD (HL),B (0x70) ---

	def "LD (HL),B stores B into memory at HL"() {
		given: "B=0x42, HL=0xD000"
		cpu.BC.setHi(0x42)
		cpu.HL.setValue(0xD000)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x70) // LD (HL),B

		when:
		cpu.execute()

		then:
		mmu.getByte(0xD000) == 0x42
	}

	// --- RLC_r: RLC B (CB 0x00) ---

	def "RLC B rotates B left, bit 7 to carry and bit 0"() {
		given: "B=0x85"
		cpu.BC.setHi(0x85)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x00) // RLC B

		when:
		cpu.execute()

		then: "B=0x0B, C=1"
		cpu.BC.getHi() == 0x0B
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- RRC_r: RRC B (CB 0x08) ---

	def "RRC B rotates B right, bit 0 to carry and bit 7"() {
		given: "B=0x01"
		cpu.BC.setHi(0x01)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x08) // RRC B

		when:
		cpu.execute()

		then: "B=0x80, C=1"
		cpu.BC.getHi() == 0x80
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- RR_r: RR B (CB 0x18) ---

	def "RR B rotates B right through carry"() {
		given: "B=0x01, carry=1"
		cpu.BC.setHi(0x01)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x18) // RR B

		when:
		cpu.execute()

		then: "B=0x80 (carry to bit 7), C=1 (old bit 0)"
		cpu.BC.getHi() == 0x80
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- SRA_r: SRA B (CB 0x28) ---

	def "SRA B arithmetic shift right preserves sign bit"() {
		given: "B=0x82 (1000_0010)"
		cpu.BC.setHi(0x82)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x28) // SRA B

		when:
		cpu.execute()

		then: "B=0xC1 (1100_0001), C=0"
		cpu.BC.getHi() == 0xC1
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- SRL_r: SRL B (CB 0x38) ---

	def "SRL B logical shift right, bit 7 = 0"() {
		given: "B=0x81 (1000_0001)"
		cpu.BC.setHi(0x81)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x38) // SRL B

		when:
		cpu.execute()

		then: "B=0x40, C=1"
		cpu.BC.getHi() == 0x40
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- SET_b_r: SET 0,B (CB 0xC0) ---

	def "SET 0,B sets bit 0 of B"() {
		given: "B=0x00"
		cpu.BC.setHi(0x00)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0xC0) // SET 0,B

		when:
		cpu.execute()

		then: "B=0x01"
		cpu.BC.getHi() == 0x01
	}

	// --- SLA_r: SLA B (CB 0x20) ---

	def "SLA B shifts left, bit 7 to carry"() {
		given: "B=0x81 (1000_0001)"
		cpu.BC.setHi(0x81)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x20) // SLA B

		when:
		cpu.execute()

		then: "B=0x02, C=1"
		cpu.BC.getHi() == 0x02
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}
}
