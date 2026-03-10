package com.almejo.osom.memory

import com.almejo.osom.cpu.Z80Cpu
import spock.lang.Specification

class TimerFrequencySpec extends Specification {

	MMU mmu
	Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	def "writing TAC does not corrupt TIMA value"() {
		given: "TIMA has a known value stored in RAM"
		mmu.@ram[MMU.TIMER_ADDRESS] = 0x42

		when: "writing a new frequency to TAC"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)

		then: "TIMA RAM value is unchanged"
		mmu.@ram[MMU.TIMER_ADDRESS] == 0x42
	}

	def "writing TAC stores the value in TAC register RAM"() {
		when: "writing a frequency value to TAC"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)

		then: "TAC register in RAM contains the written value"
		mmu.@ram[MMU.TIMER_CONTROLLER] == 0x05
	}

	def "frequency change detection compares against TAC not TIMA"() {
		given: "TAC is set to frequency 1 (262144Hz, value 0x05)"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)

		and: "TIMA has a different value that would confuse a buggy frequency check"
		mmu.@ram[MMU.TIMER_ADDRESS] = 0x02

		when: "writing the same TAC frequency again"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)

		then: "TIMA is not overwritten by the redundant TAC write"
		mmu.@ram[MMU.TIMER_ADDRESS] == 0x02
	}
}
