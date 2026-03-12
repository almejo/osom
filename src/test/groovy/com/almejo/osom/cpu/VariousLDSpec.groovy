package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class VariousLDSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	// --- LD (nn),SP (0x08) ---

	def "LD (nn),SP stores SP at 16-bit address"() {
		given:
		cpu.SP.setValue(0xFFF8)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x08)
		mmu.setByte(0xC001, 0x00) // low byte of address
		mmu.setByte(0xC002, 0xD0) // high byte: address = 0xD000

		when:
		cpu.execute()

		then: "SP value written as little-endian at 0xD000"
		mmu.getByte(0xD000) == 0xF8
		mmu.getByte(0xD001) == 0xFF
	}

	// --- LD A,(BC) (0x0A) ---

	def "LD A,(BC) loads byte at BC address into A"() {
		given:
		cpu.BC.setValue(0xD000)
		mmu.setByte(0xD000, 0x42)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x0A)

		when:
		cpu.execute()

		then:
		cpu.AF.getHi() == 0x42
	}

	// --- LD A,(HL-) (0x3A) ---

	def "LD A,(HL-) loads byte at HL into A and decrements HL"() {
		given:
		cpu.HL.setValue(0xD001)
		mmu.setByte(0xD001, 0xAB)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x3A)

		when:
		cpu.execute()

		then: "A=0xAB, HL decremented"
		cpu.AF.getHi() == 0xAB
		cpu.HL.getValue() == 0xD000
	}

	// --- LDH A,(C) (0xF2) ---

	def "LDH A,(C) loads byte from 0xFF00+C into A"() {
		given: "C=0x80 => reads from 0xFF80 (HRAM)"
		cpu.BC.setLo(0x80)
		mmu.setByte(0xFF80, 0x90)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xF2)

		when:
		cpu.execute()

		then:
		cpu.AF.getHi() == 0x90
	}

	// --- LD SP,HL (0xF9) ---

	def "LD SP,HL copies HL into SP"() {
		given:
		cpu.HL.setValue(0xDFF0)
		cpu.SP.setValue(0x0000)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0xF9)

		when:
		cpu.execute()

		then:
		cpu.SP.getValue() == 0xDFF0
	}

	// --- LD (BC),A (0x02) ---

	def "LD (BC),A stores A at address BC"() {
		given:
		cpu.AF.setHi(0x42)
		cpu.BC.setValue(0xD000)
		cpu.PC.setValue(0xC000)
		mmu.setByte(0xC000, 0x02)

		when:
		cpu.execute()

		then:
		mmu.getByte(0xD000) == 0x42
	}
}
