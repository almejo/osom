package com.almejo.osom.memory

import com.almejo.osom.cpu.Z80Cpu
import spock.lang.Specification

class MMUSpec extends Specification {

	private MMU mmu
	private Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	// === getWord address wrapping ===

	def "getWord at 0xFFFE reads from high RAM without overflow"() {
		given: "store bytes at 0xFFFE and 0xFFFF (both in high RAM)"
		mmu.setByte(0xFFFE, 0x34)
		mmu.setByte(0xFFFF, 0x56)

		expect: "little-endian: low=0x34 at FFFE, high=0x56 at FFFF → 0x5634"
		mmu.getWord(0xFFFE) == 0x5634
	}

	def "getWord at normal address returns correct 16-bit value"() {
		given: "store a word at 0xC000 (work RAM)"
		mmu.setByte(0xC000, 0xAB)
		mmu.setByte(0xC001, 0xCD)

		expect: "little-endian: low=0xAB at C000, high=0xCD at C001 → 0xCDAB"
		mmu.getWord(0xC000) == 0xCDAB
	}

	// === setWord address wrapping ===

	def "setWord at 0xFFFF wraps high byte write to address 0x0000"() {
		when: "write a word at 0xFFFF"
		mmu.setWord(0xFFFF, 0xABCD)

		then: "does not throw — address 0x10000 is masked to 0x0000"
		noExceptionThrown()

		and: "low byte written to 0xFFFF"
		mmu.getByte(0xFFFF) == 0xCD
	}

	def "setWord at normal address stores correct 16-bit value"() {
		when: "store 0xBEEF at 0xC000"
		mmu.setWord(0xC000, 0xBEEF)

		then: "little-endian: low=0xEF at C000, high=0xBE at C001"
		mmu.getByte(0xC000) == 0xEF
		mmu.getByte(0xC001) == 0xBE
	}
}
