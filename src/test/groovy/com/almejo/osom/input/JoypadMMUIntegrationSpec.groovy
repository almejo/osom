package com.almejo.osom.input

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class JoypadMMUIntegrationSpec extends Specification {

	Joypad joypad
	MMU mmu

	def setup() {
		joypad = new Joypad()
		mmu = new MMU(false)
		mmu.setJoypad(joypad)
	}

	def "write to 0xFF00 via MMU reaches joypad"() {
		when: "direction group selected via MMU write"
		mmu.setByte(0xFF00, 0xEF)

		and: "Right button is pressed"
		joypad.keyPressed(39) // VK_RIGHT

		then: "reading via MMU returns joypad state with Right pressed (bit 0 low)"
		(mmu.getByte(0xFF00) & 0x01) == 0
	}

	def "read from 0xFF00 via MMU returns joypad state"() {
		given: "action group selected and A pressed"
		mmu.setByte(0xFF00, 0xDF)
		joypad.keyPressed(90) // VK_Z = A

		when: "reading from 0xFF00 via MMU"
		int result = mmu.getByte(0xFF00)

		then: "A button reads as pressed (bit 0 low)"
		(result & 0x01) == 0
		and: "other action bits remain high"
		(result & 0x0E) == 0x0E
	}

	def "full round-trip: write select, press button, read via MMU"() {
		when: "direction group selected via MMU"
		mmu.setByte(0xFF00, 0xEF)

		and: "Up key is pressed via joypad"
		joypad.keyPressed(38) // VK_UP

		and: "register is read via MMU"
		int result = mmu.getByte(0xFF00)

		then: "complete round-trip returns correct bit pattern"
		(result & 0xC0) == 0xC0  // bits 7-6 always 1
		(result & 0x30) == 0x20  // bit 5 = 1 (action not selected), bit 4 = 0 (direction selected)
		(result & 0x04) == 0     // bit 2 = 0 (Up pressed, active low)
		(result & 0x0B) == 0x0B  // other direction bits high (Right=1, Left=1, Down=1)
	}

	def "multiple button round-trip via MMU"() {
		given: "action group selected via MMU"
		mmu.setByte(0xFF00, 0xDF)

		when: "Start and B are pressed"
		joypad.keyPressed(10) // VK_ENTER = Start
		joypad.keyPressed(88) // VK_X = B

		then: "reading via MMU shows both pressed"
		int result = mmu.getByte(0xFF00)
		(result & 0x02) == 0  // B (bit 1) pressed
		(result & 0x08) == 0  // Start (bit 3) pressed
		(result & 0x05) == 0x05  // A (bit 0) and Select (bit 2) not pressed
	}

	def "null joypad guard returns 0xFF for reads"() {
		given: "MMU without joypad wired"
		MMU unwiredMmu = new MMU(false)

		expect: "reading 0xFF00 returns 0xFF (no joypad connected)"
		unwiredMmu.getByte(0xFF00) == 0xFF
	}

	def "null joypad guard ignores writes silently"() {
		given: "MMU without joypad wired"
		MMU unwiredMmu = new MMU(false)

		when: "writing to 0xFF00"
		unwiredMmu.setByte(0xFF00, 0xEF)

		then: "no exception thrown, reads still return 0xFF"
		unwiredMmu.getByte(0xFF00) == 0xFF
	}

}
