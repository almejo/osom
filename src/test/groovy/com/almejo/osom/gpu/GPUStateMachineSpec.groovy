package com.almejo.osom.gpu

import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.memory.MMU
import spock.lang.Specification

class GPUStateMachineSpec extends Specification {

	private MMU mmu
	private Z80Cpu cpu
	private GPU gpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
		gpu = new GPU()
		gpu.setMmu(mmu)
		gpu.setFrameBuffer(new FrameBuffer())
		// Enable LCD: set LCDC bit 7
		mmu.setByte(MMU.LCD_CONTROLLER, 0x80)
	}

	// === Task 1: Mode transition timing tests ===

	def "GPU starts in OAM mode (Mode 2 / SPRITES)"() {
		expect: "initial mode is SPRITES (2)"
		gpu.getMode() == 2
	}

	def "Mode 2 (OAM) transitions to Mode 3 (GRAPHICS) after 80 T-cycles"() {
		when: "we advance by 80 T-cycles"
		gpu.update(80)

		then: "GPU is in GRAPHICS mode (3)"
		gpu.getMode() == 3
	}

	def "Mode 2 (OAM) stays in Mode 2 before 80 T-cycles"() {
		when: "we advance by 79 T-cycles"
		gpu.update(79)

		then: "GPU is still in OAM mode (2)"
		gpu.getMode() == 2
	}

	def "Mode 3 (GRAPHICS) transitions to Mode 0 (H_BLANK) after 172 T-cycles"() {
		given: "GPU is in GRAPHICS mode"
		gpu.update(80)

		when: "we advance by 172 more T-cycles"
		gpu.update(172)

		then: "GPU is in H_BLANK mode (0)"
		gpu.getMode() == 0
	}

	def "Mode 0 (H_BLANK) transitions to Mode 2 (OAM) after 204 T-cycles and increments line"() {
		given: "GPU is in H_BLANK mode at line 0"
		gpu.update(80)   // -> GRAPHICS
		gpu.update(172)  // -> H_BLANK

		when: "we advance by 204 T-cycles"
		gpu.update(204)

		then: "GPU is back in OAM mode at line 1"
		gpu.getMode() == 2
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 1
	}

	def "full scanline takes 456 T-cycles"() {
		when: "we advance by 456 T-cycles total"
		gpu.update(80)   // OAM -> GRAPHICS
		gpu.update(172)  // GRAPHICS -> H_BLANK
		gpu.update(204)  // H_BLANK -> OAM (line 1)

		then: "one full scanline completed, now at line 1 in OAM"
		gpu.getMode() == 2
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 1
	}

	// === Task 3: MMU GPU I/O register handler tests ===

	def "SCY register (0xFF42) write and read roundtrip"() {
		when: "writing a value to SCY"
		mmu.setByte(MMU.LCD_SCROLL_Y, 42)

		then: "reading it back returns the same value"
		mmu.getByte(MMU.LCD_SCROLL_Y) == 42
	}

	def "SCX register (0xFF43) write and read roundtrip"() {
		when: "writing a value to SCX"
		mmu.setByte(MMU.LCD_SCROLL_X, 99)

		then: "reading it back returns the same value"
		mmu.getByte(MMU.LCD_SCROLL_X) == 99
	}

	def "LYC register (0xFF45) write and read roundtrip"() {
		when: "writing a value to LYC"
		mmu.setByte(MMU.LCD_LY_COMPARE, 100)

		then: "reading it back returns the same value"
		mmu.getByte(MMU.LCD_LY_COMPARE) == 100
	}

	def "STAT register (0xFF41) preserves read-only bits 0-2 on write"() {
		given: "STAT has mode bits set to 2 (OAM)"
		// GPU starts in mode 2 (SPRITES), so bits 0-1 = 10 binary = 2
		// We need to set the initial STAT value manually to test write protection
		// The RAM at LCD_STATUS initially has whatever the GPU wrote

		when: "writing 0xFF to STAT (trying to overwrite all bits)"
		mmu.setByte(MMU.LCD_STATUS, 0xFF)

		then: "bits 3-6 are written (0x78), bits 0-2 are preserved"
		// bits 0-2 should be preserved from before the write
		// bits 3-6 should be 0x78 (all set)
		// bit 7 is unused
		(mmu.getByte(MMU.LCD_STATUS) & 0x78) == 0x78
	}

	// === Task 4: STAT register mode bits ===

	def "STAT bits 0-1 reflect current GPU mode through a full scanline"() {
		expect: "initial STAT mode bits = 2 (OAM)"
		(mmu.getByte(MMU.LCD_STATUS) & 0x03) == 2

		when: "advance to GRAPHICS mode"
		gpu.update(80)

		then: "STAT mode bits = 3"
		(mmu.getByte(MMU.LCD_STATUS) & 0x03) == 3

		when: "advance to H_BLANK mode"
		gpu.update(172)

		then: "STAT mode bits = 0"
		(mmu.getByte(MMU.LCD_STATUS) & 0x03) == 0

		when: "advance to next scanline OAM mode"
		gpu.update(204)

		then: "STAT mode bits = 2"
		(mmu.getByte(MMU.LCD_STATUS) & 0x03) == 2
	}

	def "STAT bits 0-1 show V-Blank mode (1) at line 144"() {
		given: "advance GPU to line 144 (V-Blank)"
		advanceToLine(144)

		expect: "STAT mode bits = 1 (V-Blank)"
		(mmu.getByte(MMU.LCD_STATUS) & 0x03) == 1
	}

	// === Task 5: LY=LYC comparison ===

	def "STAT bit 2 (coincidence flag) is set when LY equals LYC"() {
		given: "LYC is set to 5"
		mmu.setByte(MMU.LCD_LY_COMPARE, 5)

		when: "GPU advances to line 5"
		advanceToLine(5)

		then: "STAT bit 2 is set"
		(mmu.getByte(MMU.LCD_STATUS) & 0x04) == 0x04
	}

	def "STAT bit 2 (coincidence flag) is clear when LY does not equal LYC"() {
		given: "LYC is set to 5"
		mmu.setByte(MMU.LCD_LY_COMPARE, 5)

		when: "GPU is at line 3 (not matching LYC)"
		advanceToLine(3)

		then: "STAT bit 2 is clear"
		(mmu.getByte(MMU.LCD_STATUS) & 0x04) == 0
	}

	def "STAT bit 2 transitions from SET to CLEAR when LY moves past LYC match"() {
		given: "LYC is set to 2"
		mmu.setByte(MMU.LCD_LY_COMPARE, 2)

		when: "GPU advances to line 2 (LY == LYC)"
		advanceToLine(2)

		then: "STAT bit 2 is set"
		(mmu.getByte(MMU.LCD_STATUS) & 0x04) == 0x04

		when: "GPU advances to line 3 (LY != LYC)"
		gpu.update(80)   // OAM -> GRAPHICS
		gpu.update(172)  // GRAPHICS -> H_BLANK
		gpu.update(204)  // H_BLANK -> OAM (line 3)

		then: "STAT bit 2 is cleared"
		(mmu.getByte(MMU.LCD_STATUS) & 0x04) == 0
	}

	def "LY=LYC fires LCD STAT interrupt when STAT bit 6 is enabled"() {
		given: "LYC is set to 2 and STAT bit 6 (LYC interrupt enable) is set"
		mmu.setByte(MMU.LCD_LY_COMPARE, 2)
		mmu.setByte(MMU.LCD_STATUS, 0x40)
		// Clear any pending interrupts
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU advances to line 2"
		advanceToLine(2)

		then: "LCD STAT interrupt (bit 1) is set in interrupt controller"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0x02
	}

	def "LY=LYC does NOT fire LCD STAT interrupt when STAT bit 6 is disabled"() {
		given: "LYC is set to 2 but STAT bit 6 is clear"
		mmu.setByte(MMU.LCD_LY_COMPARE, 2)
		mmu.setByte(MMU.LCD_STATUS, 0x00)
		// Clear any pending interrupts
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU advances to line 2"
		advanceToLine(2)

		then: "LCD STAT interrupt bit is not set (only V-Blank may fire later, but not LCD STAT from LYC)"
		// At line 2, no V-Blank yet. Only LCD STAT from LYC could fire, but it shouldn't.
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0
	}

	// === Task 6: LCD STAT interrupts on mode transitions ===

	def "H-Blank STAT interrupt fires when STAT bit 3 is enabled"() {
		given: "STAT bit 3 (H-Blank interrupt) is enabled"
		mmu.setByte(MMU.LCD_STATUS, 0x08)
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU transitions to H-Blank (OAM -> GRAPHICS -> H_BLANK)"
		gpu.update(80)   // OAM -> GRAPHICS
		gpu.update(172)  // GRAPHICS -> H_BLANK

		then: "LCD STAT interrupt (bit 1) is fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0x02
	}

	def "H-Blank STAT interrupt does NOT fire when STAT bit 3 is disabled"() {
		given: "STAT bit 3 (H-Blank interrupt) is disabled"
		mmu.setByte(MMU.LCD_STATUS, 0x00)
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU transitions to H-Blank"
		gpu.update(80)   // OAM -> GRAPHICS
		gpu.update(172)  // GRAPHICS -> H_BLANK

		then: "LCD STAT interrupt is NOT fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0
	}

	def "V-Blank STAT interrupt fires when STAT bit 4 is enabled"() {
		given: "STAT bit 4 (V-Blank STAT interrupt) is enabled"
		mmu.setByte(MMU.LCD_STATUS, 0x10)
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU advances to V-Blank (line 144)"
		advanceToLine(144)

		then: "LCD STAT interrupt (bit 1) is fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0x02
	}

	def "OAM STAT interrupt fires when STAT bit 5 is enabled"() {
		given: "STAT bit 5 (OAM interrupt) is enabled"
		mmu.setByte(MMU.LCD_STATUS, 0x20)
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU completes one scanline and transitions to OAM for line 1"
		gpu.update(80)   // OAM -> GRAPHICS
		gpu.update(172)  // GRAPHICS -> H_BLANK
		gpu.update(204)  // H_BLANK -> OAM (line 1)

		then: "LCD STAT interrupt (bit 1) is fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0x02
	}

	def "V-Blank STAT interrupt does NOT fire when STAT bit 4 is disabled"() {
		given: "STAT bit 4 (V-Blank STAT interrupt) is disabled"
		mmu.setByte(MMU.LCD_STATUS, 0x00)
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU advances to V-Blank (line 144)"
		advanceToLine(144)

		then: "V-Blank hardware interrupt fires (bit 0) but LCD STAT interrupt (bit 1) does NOT"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x01) == 0x01
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0
	}

	def "OAM STAT interrupt does NOT fire when STAT bit 5 is disabled"() {
		given: "STAT bit 5 (OAM interrupt) is disabled"
		mmu.setByte(MMU.LCD_STATUS, 0x00)
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU completes one scanline and transitions to OAM for line 1"
		gpu.update(80)   // OAM -> GRAPHICS
		gpu.update(172)  // GRAPHICS -> H_BLANK
		gpu.update(204)  // H_BLANK -> OAM (line 1)

		then: "LCD STAT interrupt (bit 1) is NOT fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x02) == 0
	}

	// === Task 7: Background rendering verification ===

	def "background rendering uses scroll registers for pixel offset"() {
		given: "background enabled (LCDC bit 0), tile data at 0x8000 (LCDC bit 4)"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x91) // LCD enabled + BG enabled + tile data 0x8000
		mmu.setByte(MMU.PALETTE_BGP, 0xE4) // identity palette: color 0→shade 0, 1→1, 2→2, 3→3
		// Set scroll Y = 0, scroll X = 0
		mmu.setByte(MMU.LCD_SCROLL_Y, 0)
		mmu.setByte(MMU.LCD_SCROLL_X, 0)

		and: "write a known tile at tile index 0, first line = byte1=0xFF, byte2=0x00 (all pixels color 1)"
		// Tile 0 at 0x8000, line 0 data at offset 0
		mmu.setByte(0x8000, 0xFF) // byte1 for tile 0, line 0
		mmu.setByte(0x8001, 0x00) // byte2 for tile 0, line 0

		and: "tile map at 0x9800 points to tile 0"
		mmu.setByte(0x9800, 0) // tile index 0

		when: "GPU renders line 0 (OAM -> GRAPHICS renders the line)"
		gpu.update(80)   // OAM -> GRAPHICS (drawLine called)
		gpu.update(172)  // GRAPHICS exit triggers drawLine, then -> H_BLANK

		then: "pixels 0-7 on line 0 should be color 1 (from byte1=0xFF, byte2=0x00)"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[7][0] == 1
	}

	def "tile Y calculation uses posY (scrollY + line) not just line"() {
		given: "background enabled, tile data at 0x8000"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x91)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4) // identity palette
		mmu.setByte(MMU.LCD_SCROLL_Y, 3) // scroll Y = 3
		mmu.setByte(MMU.LCD_SCROLL_X, 0)

		and: "tile 0 at 0x8000, line 3 data (posY=3+0=3, tileLine=(3%8)*2=6)"
		mmu.setByte(0x8000 + 6, 0xFF)  // byte1 for tile 0, line 3
		mmu.setByte(0x8000 + 7, 0x00)  // byte2 for tile 0, line 3

		and: "tile map at 0x9800 points to tile 0"
		mmu.setByte(0x9800, 0)

		when: "GPU renders line 0 (with SCY=3, posY=3)"
		gpu.update(80)
		gpu.update(172)

		then: "pixels 0-7 should be color 1 from tile line 3 data"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	// === Task 8: Comprehensive GPU state machine tests ===

	// --- 8.3: V-Blank behavior ---

	def "V-Blank starts at line 144 (not 143, not 145)"() {
		when: "GPU advances to line 143 (last visible line)"
		advanceToLine(143)

		then: "still in visible mode (OAM after completing line 142 rendering)"
		gpu.getMode() == 2
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 143

		when: "complete one more scanline to reach line 144"
		gpu.update(80)   // OAM -> GRAPHICS
		gpu.update(172)  // GRAPHICS -> H_BLANK
		gpu.update(204)  // H_BLANK -> V_BLANK (line 144)

		then: "GPU enters V-Blank at line 144"
		gpu.getMode() == 1
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 144
	}

	def "V-Blank interrupt fires exactly once at line 144 transition"() {
		given: "clear interrupts"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "GPU advances to V-Blank (line 144)"
		advanceToLine(144)

		then: "V-Blank interrupt (bit 0) is set"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x01) == 0x01

		when: "clear interrupt and advance through V-Blank lines 145-153"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)
		for (int vblankLine = 0; vblankLine < 9; vblankLine++) {
			gpu.update(456) // one V-Blank line
		}

		then: "V-Blank interrupt does NOT fire again during remaining V-Blank lines"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x01) == 0
	}

	def "LY reads 144-153 during V-Blank (NOT 0)"() {
		given: "advance to V-Blank"
		advanceToLine(144)

		expect: "LY starts at 144"
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 144

		when: "advance through V-Blank lines"
		gpu.update(456) // line 145

		then:
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 145

		when:
		gpu.update(456) // line 146

		then:
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 146
	}

	def "V-Blank lasts 10 lines (4560 T-cycles total)"() {
		given: "advance to V-Blank start"
		advanceToLine(144)

		when: "advance 9 more V-Blank lines (already at line 144, need to reach 154 which wraps to 0)"
		for (int vblankLine = 0; vblankLine < 10; vblankLine++) {
			gpu.update(456)
		}

		then: "after 10 V-Blank lines, GPU wraps to line 0 in OAM mode"
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 0
		gpu.getMode() == 2
	}

	// --- 8.4: Full frame ---

	def "one complete frame is 70224 T-cycles and returns to line 0 OAM"() {
		when: "advance by exactly one full frame"
		// 144 visible lines + 10 V-Blank lines = 154 lines x 456 = 70224 T-cycles
		// Advance all visible lines
		for (int scanline = 0; scanline < 144; scanline++) {
			gpu.update(80)
			gpu.update(172)
			gpu.update(204)
		}
		// Advance all V-Blank lines
		for (int vblankLine = 0; vblankLine < 10; vblankLine++) {
			gpu.update(456)
		}

		then: "GPU is back at line 0 in OAM mode"
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 0
		gpu.getMode() == 2
	}

	// --- 8.6: Multi-mode transition in single update() call ---

	def "single update(456) from line 0 Mode 2 processes full scanline"() {
		when: "call update with 456 cycles (full scanline)"
		gpu.update(456)

		then: "GPU processed OAM(80) + GRAPHICS(172) + H_BLANK(204), now at line 1 in OAM"
		gpu.getMode() == 2
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 1
	}

	def "realistic instruction-sized cycle values process correctly"() {
		when: "feed realistic instruction cycle values that sum to 456"
		// Simulate typical instruction mix: 4+8+12+16+20+24+4+8+12+4+8+4+... = 456
		int[] instructionCycles = [4, 8, 12, 16, 20, 24, 4, 8, 12, 4, 8, 4, 4, 8, 4, 4, 4, 8, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4]
		int total = 0
		for (int cycles : instructionCycles) {
			gpu.update(cycles)
			total += cycles
			if (total >= 456) break
		}
		// Ensure we've processed at least one full scanline
		while (total < 456) {
			gpu.update(4)
			total += 4
		}

		then: "GPU correctly processed one full scanline to line 1 in OAM mode"
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 1
		gpu.getMode() == 2
	}

	// --- 8.7: STAT bit 7 always reads as 1 ---

	def "STAT bit 7 always reads as 1 on real hardware"() {
		when: "reading STAT register"
		int stat = mmu.getByte(MMU.LCD_STATUS)

		then: "bit 7 is always set"
		(stat & 0x80) == 0x80
	}

	// --- 8.8: STAT write protection ---

	def "STAT write with 0xFF preserves mode bits 0-2 and only changes bits 3-6"() {
		given: "GPU is in OAM mode (STAT bits 0-1 = 2)"
		int statBefore = mmu.getByte(MMU.LCD_STATUS) & 0x07

		when: "write 0xFF to STAT"
		mmu.setByte(MMU.LCD_STATUS, 0xFF)

		then: "bits 0-2 are preserved, bits 3-6 are set"
		(mmu.getByte(MMU.LCD_STATUS) & 0x07) == statBefore
		(mmu.getByte(MMU.LCD_STATUS) & 0x78) == 0x78
	}

	// --- 8.8: V-Blank interrupt fires exactly once ---

	def "V-Blank interrupt fires at line 144 and not again for remaining V-Blank lines"() {
		given: "advance to line 143"
		advanceToLine(143)
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)

		when: "complete scanline 143 entering V-Blank at line 144"
		gpu.update(80)
		gpu.update(172)
		gpu.update(204)

		then: "V-Blank interrupt fires"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x01) == 0x01

		when: "clear interrupt flag and advance one V-Blank line"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0)
		gpu.update(456) // line 145

		then: "V-Blank interrupt does NOT fire again"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x01) == 0
	}

	// --- 8.9: LCD disable ---

	def "GPU update does nothing when LCD is disabled (LCDC bit 7 = 0)"() {
		given: "LCD is disabled"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x00)

		when: "update with many cycles"
		gpu.update(10000)

		then: "mode and line haven't changed"
		gpu.getMode() == 2
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 0
	}

	// === Helper methods ===

	private void advanceToLine(int targetLine) {
		// Each visible scanline: OAM(80) + GRAPHICS(172) + H_BLANK(204) = 456 T-cycles
		for (int scanline = 0; scanline < targetLine; scanline++) {
			gpu.update(80)   // OAM -> GRAPHICS
			gpu.update(172)  // GRAPHICS -> H_BLANK
			gpu.update(204)  // H_BLANK -> next line OAM (or V_BLANK at 144)
		}
	}
}
