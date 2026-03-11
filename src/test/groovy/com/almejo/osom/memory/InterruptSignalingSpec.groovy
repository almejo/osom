package com.almejo.osom.memory

import spock.lang.Specification

class InterruptSignalingSpec extends Specification {

	MMU mmu

	def setup() {
		mmu = new MMU(false)
	}

	def "requestInterrupt(INTERRUPT_VBLANK) sets bit 0 of IF register"() {
		when: "requesting a V-Blank interrupt"
		mmu.requestInterrupt(MMU.INTERRUPT_VBLANK)

		then: "bit 0 of IF register is set"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x01
	}

	def "requestInterrupt(INTERRUPT_TIMER) sets bit 2 of IF register"() {
		when: "requesting a timer interrupt"
		mmu.requestInterrupt(MMU.INTERRUPT_TIMER)

		then: "bit 2 of IF register is set"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x04
	}

	def "requestInterrupt(INTERRUPT_LCD_STAT) sets bit 1 of IF register"() {
		when: "requesting an LCD STAT interrupt"
		mmu.requestInterrupt(MMU.INTERRUPT_LCD_STAT)

		then: "bit 1 of IF register is set"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x02
	}

	def "requestInterrupt(INTERRUPT_SERIAL) sets bit 3 of IF register"() {
		when: "requesting a serial interrupt"
		mmu.requestInterrupt(MMU.INTERRUPT_SERIAL)

		then: "bit 3 of IF register is set"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x08
	}

	def "requestInterrupt(INTERRUPT_JOYPAD) sets bit 4 of IF register"() {
		when: "requesting a joypad interrupt"
		mmu.requestInterrupt(MMU.INTERRUPT_JOYPAD)

		then: "bit 4 of IF register is set"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x10
	}

	def "multiple requestInterrupt calls preserve existing bits"() {
		when: "requesting V-Blank and then timer interrupts"
		mmu.requestInterrupt(MMU.INTERRUPT_VBLANK)
		mmu.requestInterrupt(MMU.INTERRUPT_TIMER)

		then: "both bits 0 and 2 are set in IF register"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x05
	}

	def "all five interrupt constants have correct bit index values"() {
		expect: "each constant maps to the correct bit index"
		MMU.INTERRUPT_VBLANK == 0
		MMU.INTERRUPT_LCD_STAT == 1
		MMU.INTERRUPT_TIMER == 2
		MMU.INTERRUPT_SERIAL == 3
		MMU.INTERRUPT_JOYPAD == 4
	}

	def "requesting all five interrupts sets bits 0-4"() {
		when: "requesting all five interrupt types"
		mmu.requestInterrupt(MMU.INTERRUPT_VBLANK)
		mmu.requestInterrupt(MMU.INTERRUPT_LCD_STAT)
		mmu.requestInterrupt(MMU.INTERRUPT_TIMER)
		mmu.requestInterrupt(MMU.INTERRUPT_SERIAL)
		mmu.requestInterrupt(MMU.INTERRUPT_JOYPAD)

		then: "bits 0-4 are all set in IF register"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x1F
	}
}
