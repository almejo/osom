package com.almejo.osom.input

import spock.lang.Specification

class JoypadSpec extends Specification {

	Joypad joypad

	def setup() {
		joypad = new Joypad()
	}

	// --- Direction button select (bit 4 low) ---

	def "direction select returns direction button states"() {
		given: "direction group selected (bit 4 = 0, bit 5 = 1)"
		joypad.write(0xEF)

		when: "no buttons pressed"
		int result = joypad.read()

		then: "lower nibble is 0x0F (all buttons released)"
		(result & 0x0F) == 0x0F
	}

	def "pressing Right sets bit 0 low in direction mode"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "Right is pressed"
		joypad.keyPressed(39) // VK_RIGHT

		then: "bit 0 is 0 (active low)"
		(joypad.read() & 0x01) == 0
	}

	def "pressing Left sets bit 1 low in direction mode"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "Left is pressed"
		joypad.keyPressed(37) // VK_LEFT

		then: "bit 1 is 0 (active low)"
		(joypad.read() & 0x02) == 0
	}

	def "pressing Up sets bit 2 low in direction mode"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "Up is pressed"
		joypad.keyPressed(38) // VK_UP

		then: "bit 2 is 0 (active low)"
		(joypad.read() & 0x04) == 0
	}

	def "pressing Down sets bit 3 low in direction mode"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "Down is pressed"
		joypad.keyPressed(40) // VK_DOWN

		then: "bit 3 is 0 (active low)"
		(joypad.read() & 0x08) == 0
	}

	def "pressing multiple directions simultaneously"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "Up and Right are pressed"
		joypad.keyPressed(38) // VK_UP
		joypad.keyPressed(39) // VK_RIGHT

		then: "both bits are low"
		(joypad.read() & 0x05) == 0
		and: "other direction bits remain high"
		(joypad.read() & 0x0A) == 0x0A
	}

	// --- Action button select (bit 5 low) ---

	def "action select returns action button states"() {
		given: "action group selected (bit 4 = 1, bit 5 = 0)"
		joypad.write(0xDF)

		when: "no buttons pressed"
		int result = joypad.read()

		then: "lower nibble is 0x0F (all buttons released)"
		(result & 0x0F) == 0x0F
	}

	def "pressing A sets bit 0 low in action mode"() {
		given: "action group selected"
		joypad.write(0xDF)

		when: "A is pressed (Z key)"
		joypad.keyPressed(90) // VK_Z

		then: "bit 0 is 0 (active low)"
		(joypad.read() & 0x01) == 0
	}

	def "pressing B sets bit 1 low in action mode"() {
		given: "action group selected"
		joypad.write(0xDF)

		when: "B is pressed (X key)"
		joypad.keyPressed(88) // VK_X

		then: "bit 1 is 0 (active low)"
		(joypad.read() & 0x02) == 0
	}

	def "pressing Select sets bit 2 low in action mode"() {
		given: "action group selected"
		joypad.write(0xDF)

		when: "Select is pressed (Backspace key)"
		joypad.keyPressed(8) // VK_BACK_SPACE

		then: "bit 2 is 0 (active low)"
		(joypad.read() & 0x04) == 0
	}

	def "pressing Start sets bit 3 low in action mode"() {
		given: "action group selected"
		joypad.write(0xDF)

		when: "Start is pressed (Enter key)"
		joypad.keyPressed(10) // VK_ENTER

		then: "bit 3 is 0 (active low)"
		(joypad.read() & 0x08) == 0
	}

	// --- Group isolation (negative tests) ---

	def "action buttons do not leak through direction-only select"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "action buttons A and Start are pressed"
		joypad.keyPressed(90) // VK_Z = A
		joypad.keyPressed(10) // VK_ENTER = Start

		then: "lower nibble is 0x0F (no direction buttons affected)"
		(joypad.read() & 0x0F) == 0x0F
	}

	def "direction buttons do not leak through action-only select"() {
		given: "action group selected"
		joypad.write(0xDF)

		when: "direction buttons Up and Right are pressed"
		joypad.keyPressed(38) // VK_UP
		joypad.keyPressed(39) // VK_RIGHT

		then: "lower nibble is 0x0F (no action buttons affected)"
		(joypad.read() & 0x0F) == 0x0F
	}

	// --- No group selected ---

	def "no group selected returns 0x0F in lower nibble"() {
		given: "neither group selected (both bits 4-5 high)"
		joypad.write(0xFF)

		when: "buttons are pressed"
		joypad.keyPressed(38) // VK_UP
		joypad.keyPressed(90) // VK_Z

		then: "lower nibble is 0x0F (no buttons readable)"
		(joypad.read() & 0x0F) == 0x0F
	}

	// --- Both groups selected ---

	def "both groups selected returns OR of both groups"() {
		given: "both groups selected (both bits 4-5 low)"
		joypad.write(0xCF)

		when: "a direction and an action button are pressed"
		joypad.keyPressed(39) // VK_RIGHT (direction: bit 0)
		joypad.keyPressed(90) // VK_Z (action: A, bit 0)

		then: "bit 0 is low (pressed in both groups)"
		(joypad.read() & 0x01) == 0
		and: "other bits remain high"
		(joypad.read() & 0x0E) == 0x0E
	}

	// --- Key release ---

	def "key release restores button to released state"() {
		given: "direction group selected and Right is pressed"
		joypad.write(0xEF)
		joypad.keyPressed(39) // VK_RIGHT

		expect: "Right is pressed"
		(joypad.read() & 0x01) == 0

		when: "Right is released"
		joypad.keyReleased(39) // VK_RIGHT

		then: "bit 0 is back to 1 (not pressed)"
		(joypad.read() & 0x01) == 1
	}

	// --- All 8 keyboard mappings ---

	def "keyboard mapping: arrow keys map to D-pad directions"() {
		given: "direction group selected"
		joypad.write(0xEF)

		expect: "each arrow key maps to the correct direction bit"
		joypad.keyPressed(keyCode)
		(joypad.read() & expectedBitMask) == 0

		cleanup:
		joypad.keyReleased(keyCode)

		where:
		keyCode | expectedBitMask | description
		39      | 0x01            | "Right = bit 0"
		37      | 0x02            | "Left = bit 1"
		38      | 0x04            | "Up = bit 2"
		40      | 0x08            | "Down = bit 3"
	}

	def "keyboard mapping: Z and X map to A and B buttons"() {
		given: "action group selected"
		joypad.write(0xDF)

		expect: "Z/X map to A/B buttons"
		joypad.keyPressed(keyCode)
		(joypad.read() & expectedBitMask) == 0

		cleanup:
		joypad.keyReleased(keyCode)

		where:
		keyCode | expectedBitMask | description
		90      | 0x01            | "Z = A button (bit 0)"
		88      | 0x02            | "X = B button (bit 1)"
	}

	def "keyboard mapping: Enter and Backspace map to Start and Select"() {
		given: "action group selected"
		joypad.write(0xDF)

		expect: "Enter/Backspace map to Start/Select"
		joypad.keyPressed(keyCode)
		(joypad.read() & expectedBitMask) == 0

		cleanup:
		joypad.keyReleased(keyCode)

		where:
		keyCode | expectedBitMask | description
		10      | 0x08            | "Enter = Start (bit 3)"
		8       | 0x04            | "Backspace = Select (bit 2)"
	}

	// --- Unmapped key ---

	def "unmapped key has no effect on button state"() {
		given: "both groups selected"
		joypad.write(0xCF)

		when: "an unmapped key is pressed (Q = 81)"
		joypad.keyPressed(81) // VK_Q

		then: "all buttons remain released"
		(joypad.read() & 0x0F) == 0x0F
	}

	// --- Active-low representation ---

	def "no buttons pressed reads as 0x0F in lower nibble"() {
		given: "direction group selected, no buttons pressed"
		joypad.write(0xEF)

		expect: "lower nibble is 0x0F (all bits high = all released)"
		(joypad.read() & 0x0F) == 0x0F
	}

	def "all direction buttons pressed reads as 0x00 in lower nibble"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "all 4 direction buttons are pressed"
		joypad.keyPressed(38) // Up
		joypad.keyPressed(40) // Down
		joypad.keyPressed(37) // Left
		joypad.keyPressed(39) // Right

		then: "lower nibble is 0x00 (all bits low = all pressed)"
		(joypad.read() & 0x0F) == 0x00
	}

	// --- Upper bits ---

	def "bits 7-6 are always 1"() {
		given: "direction group selected"
		joypad.write(0xEF)

		expect: "bits 7-6 are set"
		(joypad.read() & 0xC0) == 0xC0
	}

	def "bits 5-4 echo the select state"() {
		when: "direction group selected (bit 4 = 0, bit 5 = 1)"
		joypad.write(0xEF)

		then: "bits 5-4 echo: 0x20 (bit 5 = 1, bit 4 = 0)"
		(joypad.read() & 0x30) == 0x20

		when: "action group selected (bit 4 = 1, bit 5 = 0)"
		joypad.write(0xDF)

		then: "bits 5-4 echo: 0x10 (bit 5 = 0, bit 4 = 1)"
		(joypad.read() & 0x30) == 0x10
	}

	// --- Rapid press/release ---

	def "rapid press and release leaves button in released state"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "Right is pressed and immediately released"
		joypad.keyPressed(39)
		joypad.keyReleased(39)

		then: "button is released (bit 0 is 1)"
		(joypad.read() & 0x01) == 1
	}

	// --- Simultaneous opposing directions ---

	def "simultaneous opposing directions are both reported as pressed"() {
		given: "direction group selected"
		joypad.write(0xEF)

		when: "Left AND Right are pressed simultaneously"
		joypad.keyPressed(37) // Left
		joypad.keyPressed(39) // Right

		then: "both Left (bit 1) and Right (bit 0) are pressed (low)"
		(joypad.read() & 0x03) == 0
	}

	// --- isButtonPressed ---

	def "isButtonPressed returns true for pressed buttons"() {
		when: "A button is pressed"
		joypad.keyPressed(90) // VK_Z = A

		then:
		joypad.isButtonPressed(Joypad.BUTTON_A)
		!joypad.isButtonPressed(Joypad.BUTTON_B)
	}

	def "isButtonPressed returns false for invalid button index"() {
		expect:
		!joypad.isButtonPressed(-1)
		!joypad.isButtonPressed(8)
		!joypad.isButtonPressed(100)
	}

}
