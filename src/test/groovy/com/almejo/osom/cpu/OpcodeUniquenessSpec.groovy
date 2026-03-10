package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class OpcodeUniquenessSpec extends Specification {

	def "all registered standard opcodes have unique codes"() {
		given: "a Z80Cpu triggers all opcode registrations with collision detection"
		def mmu = Stub(MMU)

		when: "constructing the CPU registers all standard opcodes"
		def cpu = new Z80Cpu(mmu, 4194304)

		then: "no IllegalStateException means no opcode collisions"
		cpu.@operations.size() > 0
	}

	def "all registered CB-prefixed opcodes have unique codes"() {
		given: "a Z80Cpu triggers all opcode registrations with collision detection"
		def mmu = Stub(MMU)

		when: "constructing the CPU registers all CB-prefixed opcodes"
		def cpu = new Z80Cpu(mmu, 4194304)

		then: "no IllegalStateException means no CB opcode collisions"
		cpu.@operationsCB.size() > 0
	}
}
