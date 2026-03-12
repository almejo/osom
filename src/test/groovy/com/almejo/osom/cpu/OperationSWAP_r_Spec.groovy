package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class OperationSWAP_r_Spec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	def "SWAP A (CB 0x37) swaps nibbles of A"() {
		given: "A contains 0xF1"
		cpu.AF.setHi(0xF1)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x37)
		int initialClockT = cpu.clock.getT()

		when: "instruction executes"
		cpu.execute()

		then: "A contains nibble-swapped value 0x1F"
		cpu.AF.getHi() == 0x1F

		and: "PC advanced by 2 (CB prefix + 1-byte opcode)"
		cpu.PC.getValue() == 0xC002

		and: "cycle count is 8 T-cycles"
		cpu.clock.getT() - initialClockT == 8

		and: "flags: Z=0, N=0, H=0, C=0"
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "SWAP A (CB 0x37) sets zero flag when A is 0x00"() {
		given: "A contains 0x00"
		cpu.AF.setHi(0x00)
		cpu.AF.setLo(0xF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x37)

		when: "instruction executes"
		cpu.execute()

		then: "A is still 0x00"
		cpu.AF.getHi() == 0x00

		and: "flags: Z=1, N=0, H=0, C=0"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "SWAP A (CB 0x37) clears all flags except Z when result is non-zero"() {
		given: "A contains 0x12 and all flags are set"
		cpu.AF.setHi(0x12)
		cpu.AF.setLo(0xF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x37)

		when: "instruction executes"
		cpu.execute()

		then: "A contains nibble-swapped value 0x21"
		cpu.AF.getHi() == 0x21

		and: "all flags cleared"
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "SWAP A (CB 0x37) with symmetric nibbles"() {
		given: "A contains 0xAA (symmetric nibbles)"
		cpu.AF.setHi(0xAA)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x37)

		when: "instruction executes"
		cpu.execute()

		then: "A is unchanged since nibbles are identical"
		cpu.AF.getHi() == 0xAA

		and: "Z flag not set since result is non-zero"
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "SWAP A (CB 0x37) writes to A register, not another register"() {
		given: "A=0x12, B=0xFF — verifying SWAP writes to correct register"
		cpu.AF.setHi(0x12)
		cpu.BC.setHi(0xFF)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, 0x37)

		when: "instruction executes"
		cpu.execute()

		then: "A contains the swapped value"
		cpu.AF.getHi() == 0x21

		and: "B is untouched"
		cpu.BC.getHi() == 0xFF
	}
}
