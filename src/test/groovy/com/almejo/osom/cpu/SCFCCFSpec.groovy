package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class SCFCCFSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- SCF (0x37) ---

	def "SCF sets carry flag and clears N and H"() {
		given: "all flags clear"
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x37)

		when:
		cpu.execute()

		then: "C=1, N=0, H=0"
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
	}

	def "SCF does not affect zero flag"() {
		given: "Z flag set before"
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x37)

		when:
		cpu.execute()

		then: "Z remains set"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}

	// --- CCF (0x3F) ---

	def "CCF toggles carry flag from 0 to 1"() {
		given: "carry clear"
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true)
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x3F)

		when:
		cpu.execute()

		then: "C=1, N=0, H=0"
		cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
		!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)
		!cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)
	}

	def "CCF toggles carry flag from 1 to 0"() {
		given: "carry set"
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x3F)

		when:
		cpu.execute()

		then: "C=0"
		!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)
	}

	def "CCF does not affect zero flag"() {
		given: "Z flag set"
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x3F)

		when:
		cpu.execute()

		then: "Z remains"
		cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)
	}
}
