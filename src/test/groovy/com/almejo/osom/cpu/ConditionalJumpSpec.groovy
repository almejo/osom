package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class ConditionalJumpSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- JP NZ,nn (0xC2) ---

	def "JP NZ jumps to address when Z=0"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xC2)
		mmu.setByte(0xC001, 0x34) // low byte
		mmu.setByte(0xC002, 0x12) // high byte
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false)
		int initialT = cpu.clock.getT()

		when:
		cpu.execute()

		then: "PC=0x1234, taken: 16 T-cycles"
		cpu.PC.getValue() == 0x1234
		cpu.clock.getT() - initialT == 16
	}

	def "JP NZ does NOT jump when Z=1"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xC2)
		mmu.setByte(0xC001, 0x34)
		mmu.setByte(0xC002, 0x12)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		int initialT = cpu.clock.getT()

		when:
		cpu.execute()

		then: "PC=0xC003, not-taken: 12 T-cycles"
		cpu.PC.getValue() == 0xC003
		cpu.clock.getT() - initialT == 12
	}

	// --- JP NC,nn (0xD2) ---

	def "JP NC jumps when C=0"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xD2)
		mmu.setByte(0xC001, 0x34)
		mmu.setByte(0xC002, 0x12)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
	}

	def "JP NC does NOT jump when C=1"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xD2)
		mmu.setByte(0xC001, 0x34)
		mmu.setByte(0xC002, 0x12)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC003
	}

	// --- JP C,nn (0xDA) ---

	def "JP C jumps when C=1"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xDA)
		mmu.setByte(0xC001, 0x34)
		mmu.setByte(0xC002, 0x12)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
	}

	def "JP C does NOT jump when C=0"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xDA)
		mmu.setByte(0xC001, 0x34)
		mmu.setByte(0xC002, 0x12)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC003
	}

	// --- JR NC,n (0x30) ---

	def "JR NC jumps with positive offset when C=0"() {
		given: "offset = +5"
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x30)
		mmu.setByte(0xC001, 0x05) // +5
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		int initialT = cpu.clock.getT()

		when:
		cpu.execute()

		then: "PC = 0xC000 + 2 + 5 = 0xC007, taken: 12 T-cycles"
		cpu.PC.getValue() == 0xC007
		cpu.clock.getT() - initialT == 12
	}

	def "JR NC does NOT jump when C=1"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x30)
		mmu.setByte(0xC001, 0x05)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		int initialT = cpu.clock.getT()

		when:
		cpu.execute()

		then: "PC=0xC002, not-taken: 8 T-cycles"
		cpu.PC.getValue() == 0xC002
		cpu.clock.getT() - initialT == 8
	}

	// --- JR C,n (0x38) ---

	def "JR C jumps with negative offset when C=1"() {
		given: "offset = -5 (0xFB signed)"
		cpu.PC.setValue(0xC010)
		mmu.setByte(0xC010, 0x38)
		mmu.setByte(0xC011, 0xFB) // -5
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when:
		cpu.execute()

		then: "PC = 0xC010 + 2 + (-5) = 0xC00D"
		cpu.PC.getValue() == 0xC00D
	}

	def "JR C does NOT jump when C=0"() {
		given:
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x38)
		mmu.setByte(0xC001, 0x05)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC002
	}
}
