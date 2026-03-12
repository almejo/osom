package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class RSTSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	def "RST 00 (0xC7) pushes PC+1 and jumps to 0x0000"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xC7) // RST 00

		when:
		cpu.execute()

		then: "PC=0x0000, return address 0xC001 on stack"
		cpu.PC.getValue() == 0x0000
		cpu.SP.getValue() == 0xDFFC
		mmu.getByte(0xDFFC) == 0x01 // low byte of 0xC001
		mmu.getByte(0xDFFD) == 0xC0 // high byte
	}

	def "RST 08 (0xCF) pushes PC+1 and jumps to 0x0008"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xCF)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x0008
		cpu.SP.getValue() == 0xDFFC
	}

	def "RST 10 (0xD7) pushes PC+1 and jumps to 0x0010"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xD7)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x0010
	}

	def "RST 18 (0xDF) pushes PC+1 and jumps to 0x0018"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xDF)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x0018
	}

	def "RST 20 (0xE7) pushes PC+1 and jumps to 0x0020"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xE7)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x0020
	}

	def "RST 28 (0xEF) pushes PC+1 and jumps to 0x0028"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xEF)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x0028
	}

	def "RST 30 (0xF7) pushes PC+1 and jumps to 0x0030"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xF7)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x0030
	}

	def "RST 38 (0xFF) pushes PC+1 and jumps to 0x0038"() {
		given:
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, 0xFF) // RST 38

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x0038
		cpu.SP.getValue() == 0xDFFC
		mmu.getByte(0xDFFC) == 0x01 // low byte of 0xC001
		mmu.getByte(0xDFFD) == 0xC0 // high byte
	}
}
