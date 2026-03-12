package com.almejo.osom.gpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class BGPPaletteSpec extends Specification {

	def "BGP register 0xE4 is identity mapping (0→0, 1→1, 2→2, 3→3)"() {
		given: "BGP = 0xE4 (11_10_01_00)"
		int bgp = 0xE4

		expect: "each color index maps to itself"
		((bgp >> (0 * 2)) & 0x03) == 0
		((bgp >> (1 * 2)) & 0x03) == 1
		((bgp >> (2 * 2)) & 0x03) == 2
		((bgp >> (3 * 2)) & 0x03) == 3
	}

	def "BGP register 0x1B reverses colors (0→3, 1→2, 2→1, 3→0)"() {
		given: "BGP = 0x1B (00_01_10_11)"
		int bgp = 0x1B

		expect: "color mapping is reversed"
		((bgp >> (0 * 2)) & 0x03) == 3
		((bgp >> (1 * 2)) & 0x03) == 2
		((bgp >> (2 * 2)) & 0x03) == 1
		((bgp >> (3 * 2)) & 0x03) == 0
	}

	def "BGP register 0x00 maps all colors to shade 0 (white)"() {
		given: "BGP = 0x00"
		int bgp = 0x00

		expect: "all indices map to 0"
		((bgp >> (0 * 2)) & 0x03) == 0
		((bgp >> (1 * 2)) & 0x03) == 0
		((bgp >> (2 * 2)) & 0x03) == 0
		((bgp >> (3 * 2)) & 0x03) == 0
	}

	def "BGP register round-trip through MMU"() {
		given: "MMU initialized"
		MMU mmu = new MMU(false)

		when: "write BGP=0x1B to 0xFF47"
		mmu.setByte(MMU.PALETTE_BGP, 0x1B)

		then: "readback matches"
		mmu.getByte(MMU.PALETTE_BGP) == 0x1B
	}

	def "BGP register 0xFC maps index 0 to shade 0 and indices 1-3 to shade 3"() {
		given: "BGP = 0xFC (11_11_11_00)"
		int bgp = 0xFC

		expect:
		((bgp >> (0 * 2)) & 0x03) == 0
		((bgp >> (1 * 2)) & 0x03) == 3
		((bgp >> (2 * 2)) & 0x03) == 3
		((bgp >> (3 * 2)) & 0x03) == 3
	}
}
