package com.almejo.osom.memory

import com.almejo.osom.cpu.Z80Cpu
import spock.lang.Specification

class HighRAMSpec extends Specification {

	MMU mmu
	Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	def "write to 0xFF80 is stored and readable"() {
		when: "writing a value to the first HRAM address"
		mmu.setByte(0xFF80, 0x42)

		then: "the value is stored and can be read back"
		mmu.getByte(0xFF80) == 0x42
	}

	def "write to 0xFF81 is stored and readable"() {
		when: "writing a value to the second HRAM address"
		mmu.setByte(0xFF81, 0xAB)

		then: "the value is stored and can be read back"
		mmu.getByte(0xFF81) == 0xAB
	}

	def "write to 0xFFFE is stored and readable"() {
		when: "writing a value to the last HRAM address"
		mmu.setByte(0xFFFE, 0x99)

		then: "the value is stored and can be read back"
		mmu.getByte(0xFFFE) == 0x99
	}

	def "HRAM bytes default to zero before any write"() {
		expect: "0xFF80 reads as zero before any write"
		mmu.getByte(0xFF80) == 0
	}
}
