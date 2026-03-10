package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class TimerEnableSpec extends Specification {

	MMU mmu
	Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	def "timer is disabled when TAC bit 2 is clear"() {
		given: "TAC register has bit 2 clear (timer disabled)"
		mmu.@ram[MMU.TIMER_CONTROLLER] = 0x03

		expect: "isClockEnabled returns false"
		!cpu.isClockEnabled()
	}

	def "timer is enabled when TAC bit 2 is set"() {
		given: "TAC register has bit 2 set (timer enabled, frequency 1)"
		mmu.@ram[MMU.TIMER_CONTROLLER] = 0x05

		expect: "isClockEnabled returns true"
		cpu.isClockEnabled()
	}

	def "timer is enabled when TAC is 0x04 (enabled, frequency 0)"() {
		given: "TAC register is 0x04 (enabled, 4096Hz)"
		mmu.@ram[MMU.TIMER_CONTROLLER] = 0x04

		expect: "isClockEnabled returns true"
		cpu.isClockEnabled()
	}

	def "timer is disabled when TAC is 0x00"() {
		given: "TAC register is 0x00 (disabled)"
		mmu.@ram[MMU.TIMER_CONTROLLER] = 0x00

		expect: "isClockEnabled returns false"
		!cpu.isClockEnabled()
	}
}
