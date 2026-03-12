package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class CBaHLSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	private void setupCBInstruction(int cbOpcode, int hlAddress, int memValue) {
		cpu.PC.setValue(0xC000)
		cpu.HL.setValue(hlAddress)
		mmu.setByte(hlAddress, memValue)
		mmu.setByte(0xC000, 0xCB)
		mmu.setByte(0xC001, cbOpcode)
	}

	// --- RLC (HL) - CB 0x06 ---

	def "RLC (HL) rotates memory left, bit 7 to carry and bit 0"() {
		given:
		setupCBInstruction(0x06, 0xD000, 0x85) // 1000_0101

		when:
		cpu.execute()

		then: "result = 0x0B, C=1"
		mmu.getByte(0xD000) == 0x0B
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	// --- RRC (HL) - CB 0x0E ---

	def "RRC (HL) rotates memory right, bit 0 to carry and bit 7"() {
		given:
		setupCBInstruction(0x0E, 0xD000, 0x01) // 0000_0001

		when:
		cpu.execute()

		then: "result = 0x80, C=1"
		mmu.getByte(0xD000) == 0x80
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- RL (HL) - CB 0x16 ---

	def "RL (HL) rotates left through carry"() {
		given:
		setupCBInstruction(0x16, 0xD000, 0x80) // 1000_0000
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when:
		cpu.execute()

		then: "result = 0x01 (old carry goes to bit 0), C=1 (old bit 7)"
		mmu.getByte(0xD000) == 0x01
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- RR (HL) - CB 0x1E ---

	def "RR (HL) rotates right through carry"() {
		given:
		setupCBInstruction(0x1E, 0xD000, 0x01) // 0000_0001
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when:
		cpu.execute()

		then: "result = 0x80 (old carry to bit 7), C=1 (old bit 0)"
		mmu.getByte(0xD000) == 0x80
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- SLA (HL) - CB 0x26 ---

	def "SLA (HL) shifts left, bit 7 to carry, bit 0 = 0"() {
		given:
		setupCBInstruction(0x26, 0xD000, 0x81) // 1000_0001

		when:
		cpu.execute()

		then: "result = 0x02, C=1"
		mmu.getByte(0xD000) == 0x02
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	// --- SRA (HL) - CB 0x2E ---

	def "SRA (HL) arithmetic shift right preserves sign bit"() {
		given:
		setupCBInstruction(0x2E, 0xD000, 0x82) // 1000_0010

		when:
		cpu.execute()

		then: "result = 0xC1 (sign bit preserved), C=0"
		mmu.getByte(0xD000) == 0xC1
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- SWAP (HL) - CB 0x36 ---

	def "SWAP (HL) swaps nibbles"() {
		given:
		setupCBInstruction(0x36, 0xD000, 0xF1)

		when:
		cpu.execute()

		then: "result = 0x1F"
		mmu.getByte(0xD000) == 0x1F
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "SWAP (HL) with 0x00 sets zero flag"() {
		given:
		setupCBInstruction(0x36, 0xD000, 0x00)

		when:
		cpu.execute()

		then:
		mmu.getByte(0xD000) == 0x00
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	// --- SRL (HL) - CB 0x3E ---

	def "SRL (HL) logical shift right, bit 0 to carry, bit 7 = 0"() {
		given:
		setupCBInstruction(0x3E, 0xD000, 0x81) // 1000_0001

		when:
		cpu.execute()

		then: "result = 0x40, C=1"
		mmu.getByte(0xD000) == 0x40
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	// --- BIT n,(HL) - CB 0x46 ---

	def "BIT 0,(HL) sets Z flag when bit 0 is clear"() {
		given:
		setupCBInstruction(0x46, 0xD000, 0xFE) // bit 0 = 0

		when:
		cpu.execute()

		then: "Z=1, H=1, N=0"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
		cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
	}

	def "BIT 0,(HL) clears Z flag when bit 0 is set"() {
		given:
		setupCBInstruction(0x46, 0xD000, 0x01) // bit 0 = 1

		when:
		cpu.execute()

		then: "Z=0"
		!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	// --- RES n,(HL) - CB 0x86 ---

	def "RES 0,(HL) clears bit 0 in memory"() {
		given:
		setupCBInstruction(0x86, 0xD000, 0xFF)

		when:
		cpu.execute()

		then: "bit 0 cleared"
		mmu.getByte(0xD000) == 0xFE
	}

	// --- SET n,(HL) - CB 0xC6 ---

	def "SET 0,(HL) sets bit 0 in memory"() {
		given:
		setupCBInstruction(0xC6, 0xD000, 0x00)

		when:
		cpu.execute()

		then: "bit 0 set"
		mmu.getByte(0xD000) == 0x01
	}

	def "SET 7,(HL) sets bit 7 in memory"() {
		given:
		setupCBInstruction(0xFE, 0xD000, 0x00)

		when:
		cpu.execute()

		then: "bit 7 set"
		mmu.getByte(0xD000) == 0x80
	}
}
