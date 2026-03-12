package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class DAASpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	def "DAA after addition: 0x15 + 0x27 = BCD 0x42"() {
		given: "A=0x3C (raw result of 0x15+0x27), N=0, H=1 (half-carry from 5+7)"
		cpu.AF.setHi(0x3C)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x27) // DAA

		when:
		cpu.execute()

		then: "A=0x42 (BCD correction adds 0x06)"
		cpu.AF.getHi() == 0x42
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "DAA after addition: lower nibble > 9 triggers correction"() {
		given: "A=0x0A (e.g., 0x05+0x05 without half-carry), N=0"
		cpu.AF.setHi(0x0A)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x27)

		when:
		cpu.execute()

		then: "A=0x10 (0x0A + 0x06 = 0x10)"
		cpu.AF.getHi() == 0x10
	}

	def "DAA after addition: upper nibble > 9 triggers carry"() {
		given: "A=0xA0 (e.g., 50+50=100 decimal), N=0"
		cpu.AF.setHi(0xA0)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x27)

		when:
		cpu.execute()

		then: "A=0x00 (0xA0 + 0x60 = 0x100), C=1, Z=1"
		cpu.AF.getHi() == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "DAA after subtraction: half-carry correction"() {
		given: "A=0xFA (raw result of BCD sub with half-carry borrow), N=1, H=1"
		cpu.AF.setHi(0xFA)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x27)

		when:
		cpu.execute()

		then: "A = (0xFA - 0x06) & 0xFF = 0xF4"
		cpu.AF.getHi() == 0xF4
	}

	def "DAA after subtraction: carry correction"() {
		given: "A=0x60, N=1, C=1"
		cpu.AF.setHi(0x60)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x27)

		when:
		cpu.execute()

		then: "A = 0x60 - 0x60 = 0x00, Z=1"
		cpu.AF.getHi() == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "DAA always clears half-carry flag"() {
		given: "H flag set before DAA"
		cpu.AF.setHi(0x00)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x27)

		when:
		cpu.execute()

		then: "H=0 after DAA"
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
	}

	def "DAA with carry-only input sets carry in output"() {
		given: "A=0x00, carry set (e.g., after 99+01=100 BCD)"
		cpu.AF.setHi(0x00)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x27)

		when:
		cpu.execute()

		then: "A=0x60, carry remains set"
		cpu.AF.getHi() == 0x60
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}
}
