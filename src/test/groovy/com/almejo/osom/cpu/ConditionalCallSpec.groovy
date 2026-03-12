package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class ConditionalCallSpec extends Specification {
	Z80Cpu cpu
	MMU mmu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
	}

	private void setupCallInstruction(int opcode, int targetAddress) {
		cpu.PC.setValue(0xC000)
		cpu.SP.setValue(0xDFFE)
		mmu.setByte(0xC000, opcode)
		mmu.setByte(0xC001, targetAddress & 0xFF)        // low byte
		mmu.setByte(0xC002, (targetAddress >> 8) & 0xFF)  // high byte
	}

	// --- CALL NZ,nn (0xC4) ---

	def "CALL NZ pushes PC+3 and jumps when Z=0"() {
		given:
		setupCallInstruction(0xC4, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false)
		int initialT = cpu.clock.getT()

		when:
		cpu.execute()

		then: "PC=0x1234, return address 0xC003 on stack"
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFC
		mmu.getByte(0xDFFC) == 0x03 // low byte of 0xC003
		mmu.getByte(0xDFFD) == 0xC0 // high byte
		cpu.clock.getT() - initialT == 24
	}

	def "CALL NZ does NOT call when Z=1"() {
		given:
		setupCallInstruction(0xC4, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)
		int initialT = cpu.clock.getT()

		when:
		cpu.execute()

		then: "PC=0xC003, SP unchanged"
		cpu.PC.getValue() == 0xC003
		cpu.SP.getValue() == 0xDFFE
		cpu.clock.getT() - initialT == 12
	}

	// --- CALL Z,nn (0xCC) ---

	def "CALL Z pushes and jumps when Z=1"() {
		given:
		setupCallInstruction(0xCC, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, true)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFC
	}

	def "CALL Z does NOT call when Z=0"() {
		given:
		setupCallInstruction(0xCC, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC003
		cpu.SP.getValue() == 0xDFFE
	}

	// --- CALL NC,nn (0xD4) ---

	def "CALL NC pushes and jumps when C=0"() {
		given:
		setupCallInstruction(0xD4, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFC
	}

	def "CALL NC does NOT call when C=1"() {
		given:
		setupCallInstruction(0xD4, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC003
		cpu.SP.getValue() == 0xDFFE
	}

	// --- CALL C,nn (0xDC) ---

	def "CALL C pushes and jumps when C=1"() {
		given:
		setupCallInstruction(0xDC, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0x1234
		cpu.SP.getValue() == 0xDFFC
	}

	def "CALL C does NOT call when C=0"() {
		given:
		setupCallInstruction(0xDC, 0x1234)
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false)

		when:
		cpu.execute()

		then:
		cpu.PC.getValue() == 0xC003
		cpu.SP.getValue() == 0xDFFE
	}
}
