package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class ALUCarryAwareSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- ADC tests ---

	def "adcRegisterHI adds value and carry=0 correctly"() {
		given: "A=0x10, carry flag clear"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "ADC A, 0x05"
		cpu.alu.adcRegisterHI(cpu.AF, 0x05)

		then: "A=0x15, Z=0, N=0, H=0, C=0"
		cpu.AF.getHi() == 0x15
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "adcRegisterHI adds value and carry=1 correctly"() {
		given: "A=0x10, carry flag set"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when: "ADC A, 0x05 with carry"
		cpu.alu.adcRegisterHI(cpu.AF, 0x05)

		then: "A=0x16 (0x10+0x05+1), Z=0, N=0, H=0, C=0"
		cpu.AF.getHi() == 0x16
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "adcRegisterHI sets zero flag when result is 0"() {
		given: "A=0xFF, carry flag set"
		cpu.AF.setHi(0xFF)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when: "ADC A, 0x00 with carry => 0xFF+0+1=0x100 => 0x00"
		cpu.alu.adcRegisterHI(cpu.AF, 0x00)

		then: "A=0x00, Z=1, N=0, H=1, C=1"
		cpu.AF.getHi() == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "adcRegisterHI sets half-carry flag on nibble overflow"() {
		given: "A=0x0F, carry flag clear"
		cpu.AF.setHi(0x0F)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "ADC A, 0x01"
		cpu.alu.adcRegisterHI(cpu.AF, 0x01)

		then: "A=0x10, H=1"
		cpu.AF.getHi() == 0x10
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "adcRegisterHI sets carry flag on byte overflow"() {
		given: "A=0xF0, carry flag clear"
		cpu.AF.setHi(0xF0)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "ADC A, 0x20"
		cpu.alu.adcRegisterHI(cpu.AF, 0x20)

		then: "A=0x10 (overflow), C=1"
		cpu.AF.getHi() == 0x10
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "adcRegisterHI always clears subtract flag"() {
		given: "subtract flag is set"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "ADC A, 0x05"
		cpu.alu.adcRegisterHI(cpu.AF, 0x05)

		then: "N=0"
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	// --- SBC tests ---

	def "sbcRegisterHI subtracts value and carry=0 correctly"() {
		given: "A=0x10, carry flag clear"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "SBC A, 0x05 — lower nibble 0x0 < 0x5, so H=1"
		cpu.alu.sbcRegisterHI(cpu.AF, 0x05)

		then: "A=0x0B, Z=0, N=1, H=1 (nibble borrow), C=0"
		cpu.AF.getHi() == 0x0B
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "sbcRegisterHI subtracts value and carry=1 correctly"() {
		given: "A=0x10, carry flag set"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when: "SBC A, 0x05 with carry — lower nibble 0x0 < 0x5+1=0x6, so H=1"
		cpu.alu.sbcRegisterHI(cpu.AF, 0x05)

		then: "A=0x0A (0x10-0x05-1), Z=0, N=1, H=1 (nibble borrow), C=0"
		cpu.AF.getHi() == 0x0A
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "sbcRegisterHI sets zero flag when result is 0"() {
		given: "A=0x01, carry flag clear"
		cpu.AF.setHi(0x01)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "SBC A, 0x01"
		cpu.alu.sbcRegisterHI(cpu.AF, 0x01)

		then: "A=0x00, Z=1"
		cpu.AF.getHi() == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "sbcRegisterHI sets half-carry flag on nibble borrow"() {
		given: "A=0x10, carry flag clear"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "SBC A, 0x01 — lower nibble 0x0 < 0x1"
		cpu.alu.sbcRegisterHI(cpu.AF, 0x01)

		then: "H=1 (nibble borrow)"
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
	}

	def "sbcRegisterHI sets carry flag on byte underflow"() {
		given: "A=0x00, carry flag clear"
		cpu.AF.setHi(0x00)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "SBC A, 0x01 — underflow"
		cpu.alu.sbcRegisterHI(cpu.AF, 0x01)

		then: "A=0xFF, C=1"
		cpu.AF.getHi() == 0xFF
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "sbcRegisterHI always sets subtract flag"() {
		given: "subtract flag is clear"
		cpu.AF.setHi(0x10)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when: "SBC A, 0x05"
		cpu.alu.sbcRegisterHI(cpu.AF, 0x05)

		then: "N=1"
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}
}
