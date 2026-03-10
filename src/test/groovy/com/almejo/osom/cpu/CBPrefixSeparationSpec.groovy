package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class CBPrefixSeparationSpec extends Specification {

	def "CB-prefixed operations are only in the CB dispatch map"() {
		given: "a Z80Cpu with all opcodes registered"
		def mmu = Stub(MMU)
		def cpu = new Z80Cpu(mmu, 4194304)

		when: "checking the CB dispatch map for non-CB operations"
		def operationsCB = cpu.@operationsCB
		def nonCBInCBMap = operationsCB.values().findAll { operation ->
			!(operation instanceof OperationCB)
		}.collect { it.class.simpleName }

		then: "all entries in the CB map are OperationCB instances"
		nonCBInCBMap == []
	}

	def "standard dispatch map does not contain CB-prefixed operations"() {
		given: "a Z80Cpu with all opcodes registered"
		def mmu = Stub(MMU)
		def cpu = new Z80Cpu(mmu, 4194304)

		when: "checking the standard dispatch map for CB operations"
		def operations = cpu.@operations
		def cbInStandardMap = operations.values().findAll { operation ->
			operation instanceof OperationCB
		}.collect { it.class.simpleName }

		then: "no CB-prefixed operations in the standard map"
		cbInStandardMap == []
	}
}
