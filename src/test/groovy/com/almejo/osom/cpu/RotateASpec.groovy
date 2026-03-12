package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class RotateASpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- RLCA (0x07) ---

	def "RLCA rotates A left, old bit 7 to carry and bit 0"() {
		given: "A=0x85 (1000_0101), bit 7 set"
		cpu.AF.setHi(0x85)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x07)

		when:
		cpu.execute()

		then: "A=0x0B (0000_1011), C=1"
		cpu.AF.getHi() == 0x0B
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
	}

	def "RLCA with bit 7 clear sets carry=0"() {
		given: "A=0x01 (0000_0001), bit 7 clear"
		cpu.AF.setHi(0x01)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x07)

		when:
		cpu.execute()

		then: "A=0x02, C=0"
		cpu.AF.getHi() == 0x02
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "RLCA always clears zero flag even when result would be zero"() {
		given: "A=0x00"
		cpu.AF.setHi(0x00)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x07)

		when:
		cpu.execute()

		then: "Z=0 always for RLCA"
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	// --- RRCA (0x0F) ---

	def "RRCA rotates A right, old bit 0 to carry and bit 7"() {
		given: "A=0x01 (0000_0001), bit 0 set"
		cpu.AF.setHi(0x01)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x0F)

		when:
		cpu.execute()

		then: "A=0x80 (1000_0000), C=1"
		cpu.AF.getHi() == 0x80
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
	}

	def "RRCA with bit 0 clear sets carry=0"() {
		given: "A=0x02 (0000_0010), bit 0 clear"
		cpu.AF.setHi(0x02)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x0F)

		when:
		cpu.execute()

		then: "A=0x01, C=0"
		cpu.AF.getHi() == 0x01
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- RRA (0x1F) ---

	def "RRA rotates A right through carry"() {
		given: "A=0x01, carry=0"
		cpu.AF.setHi(0x01)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x1F)

		when:
		cpu.execute()

		then: "A=0x00 (old carry=0 goes to bit 7, old bit 0=1 goes to carry)"
		cpu.AF.getHi() == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "RRA with carry=1 sets bit 7"() {
		given: "A=0x00, carry=1"
		cpu.AF.setHi(0x00)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x1F)

		when:
		cpu.execute()

		then: "A=0x80 (carry goes to bit 7), C=0 (old bit 0 was 0)"
		cpu.AF.getHi() == 0x80
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	def "RRA always clears Z, N, H flags"() {
		given: "all flags set"
		cpu.AF.setHi(0x01)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x1F)

		when:
		cpu.execute()

		then:
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
	}
}
