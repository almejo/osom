package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class LDrrSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	def "LD B,C (0x41) loads C into B"() {
		given:
		cpu.BC.setHi(0x00)
		cpu.BC.setLo(0x42)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x41)

		when:
		cpu.execute()

		then: "B=0x42, C unchanged"
		cpu.BC.getHi() == 0x42
		cpu.BC.getLo() == 0x42
	}

	def "LD H,L (0x65) loads L into H"() {
		given:
		cpu.HL.setHi(0x00)
		cpu.HL.setLo(0xAB)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x65)

		when:
		cpu.execute()

		then:
		cpu.HL.getHi() == 0xAB
	}

	def "LD A,A (0x7F) keeps A unchanged"() {
		given:
		cpu.AF.setHi(0x55)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x7F)

		when:
		cpu.execute()

		then:
		cpu.AF.getHi() == 0x55
	}

	def "LD D,E (0x53) loads E into D"() {
		given:
		cpu.DE.setHi(0x00)
		cpu.DE.setLo(0xFE)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x53)

		when:
		cpu.execute()

		then:
		cpu.DE.getHi() == 0xFE
	}

	def "LD A,B (0x78) loads B into A"() {
		given:
		cpu.AF.setHi(0x00)
		cpu.BC.setHi(0xCD)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x78)

		when:
		cpu.execute()

		then:
		cpu.AF.getHi() == 0xCD
	}
}
