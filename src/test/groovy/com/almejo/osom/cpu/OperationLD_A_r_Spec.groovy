package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class OperationLD_A_r_Spec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	def "LD A,C (0x79) loads C register into A"() {
		given: "C contains 0x42 and A contains 0x00, all flags set"
		cpu.BC.setLo(0x42)
		cpu.AF.setHi(0x00)
		cpu.AF.setLo(0xF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x79)
		int initialClockT = cpu.getClockT()

		when: "instruction executes"
		cpu.execute()

		then: "A contains C's value"
		cpu.AF.getHi() == 0x42

		and: "PC advanced by 1"
		cpu.PC.getValue() == 0xC001

		and: "cycle count is 4 T-cycles"
		cpu.getClockT() - initialClockT == 4

		and: "flags unchanged (all four remain set)"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "LD A,D (0x7A) loads D register into A"() {
		given: "D contains 0x55 and A contains 0x00, all flags set"
		cpu.DE.setHi(0x55)
		cpu.AF.setHi(0x00)
		cpu.AF.setLo(0xF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x7A)
		int initialClockT = cpu.getClockT()

		when: "instruction executes"
		cpu.execute()

		then: "A contains D's value"
		cpu.AF.getHi() == 0x55

		and: "PC advanced by 1"
		cpu.PC.getValue() == 0xC001

		and: "cycle count is 4 T-cycles"
		cpu.getClockT() - initialClockT == 4

		and: "flags unchanged (all four remain set)"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "LD A,D (0x7A) loads 0x00 correctly with flags preserved"() {
		given: "D contains 0x00 and A contains 0xFF, all flags set"
		cpu.DE.setHi(0x00)
		cpu.AF.setHi(0xFF)
		cpu.AF.setLo(0xF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x7A)
		int initialClockT = cpu.getClockT()

		when: "instruction executes"
		cpu.execute()

		then: "A contains 0x00"
		cpu.AF.getHi() == 0x00

		and: "PC advanced by 1"
		cpu.PC.getValue() == 0xC001

		and: "cycle count is 4 T-cycles"
		cpu.getClockT() - initialClockT == 4

		and: "flags unchanged (all four remain set)"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "LD A,D (0x7A) loads 0xFF correctly with flags preserved"() {
		given: "D contains 0xFF and A contains 0x00, all flags set"
		cpu.DE.setHi(0xFF)
		cpu.AF.setHi(0x00)
		cpu.AF.setLo(0xF0)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x7A)
		int initialClockT = cpu.getClockT()

		when: "instruction executes"
		cpu.execute()

		then: "A contains 0xFF"
		cpu.AF.getHi() == 0xFF

		and: "PC advanced by 1"
		cpu.PC.getValue() == 0xC001

		and: "cycle count is 4 T-cycles"
		cpu.getClockT() - initialClockT == 4

		and: "flags unchanged (all four remain set)"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}
}
