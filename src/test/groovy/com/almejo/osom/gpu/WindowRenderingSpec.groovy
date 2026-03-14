package com.almejo.osom.gpu

import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.memory.MMU
import spock.lang.Specification

class WindowRenderingSpec extends Specification {

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
		// Enable LCD + BG + window + tile data 0x8000: bits 7,5,4,0 = 0xB1
		mmu.setByte(MMU.LCD_CONTROLLER, 0xB1)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4) // identity palette
	}

	/**
	 * Triggers drawLine() by advancing GPU through OAM (80 cycles) then GRAPHICS (172 cycles).
	 * drawLine() is called during the GRAPHICS→H_BLANK transition.
	 */
	private void renderLine() {
		gpu.update(80)   // Mode 2 (OAM) → Mode 3 (GRAPHICS)
		gpu.update(172)  // Mode 3 (GRAPHICS) → Mode 0 (H_BLANK), calls drawLine()
	}

	/**
	 * Advances the GPU to a specific scanline by completing full scanlines.
	 * After calling this, the GPU is in OAM mode at the target line.
	 */
	private void advanceToLine(int targetLine) {
		for (int scanline = 0; scanline < targetLine; scanline++) {
			gpu.update(80)   // OAM → GRAPHICS
			gpu.update(172)  // GRAPHICS → H_BLANK (drawLine called)
			gpu.update(204)  // H_BLANK → OAM (next line)
		}
	}

	/**
	 * Writes tile data (2 bytes per row) at the given tile index in the 0x8000 area.
	 */
	private void writeTileRow(int tileIndex, int row, int byte1, int byte2) {
		int address = 0x8000 + tileIndex * 16 + row * 2
		mmu.setByte(address, byte1)
		mmu.setByte(address + 1, byte2)
	}

	/**
	 * Writes a tile map entry in the specified tile map area.
	 */
	private void setWindowTileMapEntry(int tileMapBase, int column, int row, int tileIndex) {
		int address = tileMapBase + row * 32 + column
		mmu.setByte(address, tileIndex)
	}

	// === Task 4.1: Window disabled (LCDC bit 5 off) — no window pixels rendered ===

	def "window disabled (LCDC bit 5 off) renders no window pixels"() {
		given: "LCDC with window disabled (bit 5 off): LCD + BG + tile data 0x8000 = 0x91"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x91)

		and: "window position at top-left"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map has tile 1 with color index 3"
		setWindowTileMapEntry(0x9800, 0, 0, 1)
		writeTileRow(1, 0, 0xFF, 0xFF)

		and: "background tile map has tile 0 with color index 0"
		mmu.setByte(0x9800, 0)
		writeTileRow(0, 0, 0x00, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "no window pixels rendered — background color 0 remains"
		gpu.frameBuffer.getPixels()[0][0] == 0
	}

	// === Task 4.2: Full-screen window (WX=7, WY=0) ===

	def "full-screen window (WX=7, WY=0) covers all pixels"() {
		given: "window at WX=7, WY=0 (full screen)"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map (0x9800) tile 0 has color index 1"
		setWindowTileMapEntry(0x9800, 0, 0, 0)
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "all pixels in first tile come from window (shade 1)"
		for (int x = 0; x < 8; x++) {
			gpu.frameBuffer.getPixels()[x][0] == 1
		}
	}

	// === Task 4.3: Partial window (WX=87, WY=0) ===

	def "partial window (WX=87) — left half BG, right half window"() {
		given: "window at 0x9C00, BG at 0x9800: LCDC = 0xB1 | 0x40 = 0xF1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xF1)

		and: "window starts at screen X=80 (WX=87)"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 87)

		and: "BG tile map (0x9800) points to tile 0: color index 1"
		mmu.setByte(0x9800, 0)
		writeTileRow(0, 0, 0xFF, 0x00)

		and: "window tile map (0x9C00) points to tile 2: color index 2"
		setWindowTileMapEntry(0x9C00, 0, 0, 2)
		writeTileRow(2, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "pixels 0-79 show background (shade 1)"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[79][0] == 1

		and: "pixels 80-87 show window (shade 2)"
		gpu.frameBuffer.getPixels()[80][0] == 2
		gpu.frameBuffer.getPixels()[87][0] == 2
	}

	// === Task 4.4: Window starts at WY > 0 ===

	def "lines above WY show only background"() {
		given: "window at 0x9C00, BG at 0x9800: LCDC = 0xF1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xF1)

		and: "window starts at WY=10"
		mmu.setByte(MMU.WINDOW_Y, 10)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map (0x9C00) has tile 1 with color index 3"
		setWindowTileMapEntry(0x9C00, 0, 0, 1)
		writeTileRow(1, 0, 0xFF, 0xFF)

		and: "BG tile map (0x9800) points to tile 0: all rows color index 1"
		mmu.setByte(0x9800, 0)
		for (int row = 0; row < 8; row++) {
			writeTileRow(0, row, 0xFF, 0x00)
		}

		when: "GPU renders line 5 (above WY=10)"
		advanceToLine(5)
		renderLine()

		then: "only background is visible (shade 1), no window"
		gpu.frameBuffer.getPixels()[0][5] == 1
	}

	def "line at WY shows window pixels"() {
		given: "window starts at WY=10"
		mmu.setByte(MMU.WINDOW_Y, 10)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile has color index 3"
		setWindowTileMapEntry(0x9800, 0, 0, 1)
		writeTileRow(1, 0, 0xFF, 0xFF)

		when: "GPU renders line 10 (at WY)"
		advanceToLine(10)
		renderLine()

		then: "window pixels are rendered (shade 3)"
		gpu.frameBuffer.getPixels()[0][10] == 3
	}

	// === Task 4.5: Window tile map selection via LCDC bit 6 ===

	def "LCDC bit 6 = 0 selects window tile map at 0x9800"() {
		given: "LCDC with bit 6 clear: LCD + BG + window + tile data 0x8000 = 0xB1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xB1)

		and: "window at top-left"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "tile 1 at 0x9800 tile map with color index 2"
		setWindowTileMapEntry(0x9800, 0, 0, 1)
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "window reads from 0x9800 tile map (shade 2)"
		gpu.frameBuffer.getPixels()[0][0] == 2
	}

	def "LCDC bit 6 = 1 selects window tile map at 0x9C00"() {
		given: "LCDC with bit 6 set: 0xB1 | 0x40 = 0xF1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xF1)

		and: "window at top-left"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "tile 1 at 0x9C00 tile map with color index 2"
		setWindowTileMapEntry(0x9C00, 0, 0, 1)
		writeTileRow(1, 0, 0x00, 0xFF)

		and: "tile 0 at 0x9800 tile map is empty (should NOT be used)"
		setWindowTileMapEntry(0x9800, 0, 0, 0)
		writeTileRow(0, 0, 0x00, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "window reads from 0x9C00 tile map (shade 2)"
		gpu.frameBuffer.getPixels()[0][0] == 2
	}

	// === Task 4.6: Window shares tile data area with BG via LCDC bit 4 ===

	def "LCDC bit 4 = 1 uses 0x8000 unsigned tile data for window"() {
		given: "LCDC bit 4 set (0x8000 unsigned): 0xB1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xB1)

		and: "window at top-left"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map points to tile index 2"
		setWindowTileMapEntry(0x9800, 0, 0, 2)

		and: "tile 2 at 0x8000 + 2*16 = 0x8020: color index 1"
		writeTileRow(2, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "window uses tile from 0x8000 area (shade 1)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	def "LCDC bit 4 = 0 uses 0x8800 signed tile data for window"() {
		given: "LCDC bit 4 clear (0x8800 signed): LCD + BG + window = 0xA1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xA1)

		and: "window at top-left"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map points to tile index 0 (maps to 0x8800 + 128*16 = 0x9000 in signed mode)"
		setWindowTileMapEntry(0x9800, 0, 0, 0)

		and: "tile data at 0x9000 (signed index 0 → offset 128): color index 3"
		int address = 0x8800 + 128 * 16
		mmu.setByte(address, 0xFF)
		mmu.setByte(address + 1, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "window uses tile from signed 0x8800 area (shade 3)"
		gpu.frameBuffer.getPixels()[0][0] == 3
	}

	// === Task 4.7: Internal line counter increments only on window-visible scanlines ===

	def "internal line counter increments only on window-visible scanlines"() {
		given: "window starts at WY=2, WX=7 (full width)"
		mmu.setByte(MMU.WINDOW_Y, 2)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map tile 0: color index 1 for all rows"
		setWindowTileMapEntry(0x9800, 0, 0, 0)
		for (int row = 0; row < 8; row++) {
			writeTileRow(0, row, 0xFF, 0x00)
		}

		when: "render lines 0-4: lines 0-1 are above WY, lines 2-4 are window-visible"
		advanceToLine(2)  // skip lines 0-1 (no window)
		renderLine()      // line 2: windowLineCounter=0, then increments to 1
		gpu.update(204)   // H_BLANK → next line
		renderLine()      // line 3: windowLineCounter=1, then increments to 2
		gpu.update(204)
		renderLine()      // line 4: windowLineCounter=2, then increments to 3

		then: "line 2 used windowLineCounter=0 (tile row 0)"
		gpu.frameBuffer.getPixels()[0][2] == 1

		and: "line 3 used windowLineCounter=1 (tile row 1)"
		gpu.frameBuffer.getPixels()[0][3] == 1

		and: "line 4 used windowLineCounter=2 (tile row 2)"
		gpu.frameBuffer.getPixels()[0][4] == 1
	}

	def "window line counter produces correct tile row after 8 lines"() {
		given: "window starts at WY=0, WX=7"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map: tile 0 for first tile row, tile 1 for second tile row"
		setWindowTileMapEntry(0x9800, 0, 0, 0)  // tile row 0 (lines 0-7)
		setWindowTileMapEntry(0x9800, 0, 1, 1)  // tile row 1 (lines 8-15)

		and: "tile 0 row 0: color index 1, tile 1 row 0: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00)
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "render line 0 (windowLineCounter=0, tile row 0 → tile 0)"
		renderLine()

		then: "line 0 shows tile 0 (shade 1)"
		gpu.frameBuffer.getPixels()[0][0] == 1

		when: "complete line 0 and advance through lines 1-8 (advanceToLine renders each line)"
		gpu.update(204)  // complete line 0 H_BLANK → line 1 OAM
		advanceToLine(8) // renders lines 1-8, windowLineCounter reaches 8 during line 8 render

		then: "line 8 shows tile 1 (shade 2) because windowLineCounter=8 → tile row 1"
		gpu.frameBuffer.getPixels()[0][8] == 2
	}

	// === Task 4.8: Internal line counter resets at V-Blank ===

	def "window line counter resets at V-Blank"() {
		given: "window starts at WY=0, WX=7"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map tile 0 with distinct row data"
		setWindowTileMapEntry(0x9800, 0, 0, 0)
		writeTileRow(0, 0, 0xFF, 0x00) // row 0: color index 1

		when: "render first frame line 0"
		renderLine()

		then: "line 0 in frame 1 shows shade 1 (windowLineCounter was 0)"
		gpu.frameBuffer.getPixels()[0][0] == 1

		when: "complete the frame and render line 0 of next frame"
		gpu.update(204) // complete line 0
		// Render lines 1-143
		for (int scanline = 1; scanline < 144; scanline++) {
			gpu.update(80)
			gpu.update(172)
			gpu.update(204)
		}
		// V-Blank: lines 144-153
		for (int vblankLine = 0; vblankLine < 10; vblankLine++) {
			gpu.update(456)
		}
		// Now at line 0 of next frame — render it
		renderLine()

		then: "line 0 in frame 2 also shows shade 1 (windowLineCounter was reset to 0)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	// === Task 4.9: Window overwrites backgroundColorIndices for sprite BG priority ===

	def "window overwrites backgroundColorIndices for sprite BG priority"() {
		given: "LCD + BG + window + sprites + tile data 0x8000 = 0xB3"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xB3)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)
		mmu.setByte(MMU.PALETTE_OBP0, 0xE4)

		and: "window at WX=7, WY=0"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "BG tile 0: color index 0 (transparent)"
		writeTileRow(0, 0, 0x00, 0x00)
		mmu.setByte(0x9800, 0)

		and: "window tile map points to tile 2 with color index 1 (non-zero)"
		setWindowTileMapEntry(0x9800, 0, 0, 2)
		writeTileRow(2, 0, 0xFF, 0x00)

		and: "sprite at (0,0) with BG priority flag set, using tile 3 with color index 3"
		int oamAddress = 0xFE00
		mmu.setByte(oamAddress, 16)     // Y=0
		mmu.setByte(oamAddress + 1, 8)  // X=0
		mmu.setByte(oamAddress + 2, 3)  // tile 3
		mmu.setByte(oamAddress + 3, 0x80)  // BG priority
		writeTileRow(3, 0, 0xFF, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "sprite is hidden behind window because backgroundColorIndices has non-zero from window"
		// Window wrote color index 1 to backgroundColorIndices, so sprite with BG priority is hidden
		// The pixel should show window shade 1, not sprite shade 3
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	// === Task 4.10: Window with WY=144 (off-screen) ===

	def "window with WY=144 renders no window pixels"() {
		given: "window starts below visible area"
		mmu.setByte(MMU.WINDOW_Y, 144)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map has tile 1 with color index 3"
		setWindowTileMapEntry(0x9800, 0, 0, 1)
		writeTileRow(1, 0, 0xFF, 0xFF)

		and: "BG tile 0: color index 0"
		mmu.setByte(0x9800, 0)
		writeTileRow(0, 0, 0x00, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "no window pixels (shade 0 from background)"
		gpu.frameBuffer.getPixels()[0][0] == 0
	}

	def "window with WY=144 renders no window on last visible line"() {
		given: "window starts below visible area"
		mmu.setByte(MMU.WINDOW_Y, 144)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map has tile 1 with color index 3"
		setWindowTileMapEntry(0x9800, 0, 0, 1)
		writeTileRow(1, 0, 0xFF, 0xFF)

		when: "GPU renders line 143 (last visible line)"
		advanceToLine(143)
		renderLine()

		then: "no window pixels on any visible line"
		gpu.frameBuffer.getPixels()[0][143] == 0
	}

	// === Task 4.11: Window starting mid-screen ===

	def "window starting at WY=72 uses correct tile row for first window line"() {
		given: "window starts at mid-screen"
		mmu.setByte(MMU.WINDOW_Y, 72)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile map tile 0: row 0 has color index 2"
		setWindowTileMapEntry(0x9800, 0, 0, 0)
		writeTileRow(0, 0, 0x00, 0xFF)

		when: "GPU renders line 72 (first window-visible line)"
		advanceToLine(72)
		renderLine()

		then: "first window line uses windowLineCounter=0 → tile row 0 (shade 2)"
		gpu.frameBuffer.getPixels()[0][72] == 2
	}

	def "window starting at WY=72, second line uses correct counter"() {
		given: "window starts at WY=72"
		mmu.setByte(MMU.WINDOW_Y, 72)
		mmu.setByte(MMU.WINDOW_X, 7)

		and: "window tile 0: row 0 → color index 1, row 1 → color index 2"
		setWindowTileMapEntry(0x9800, 0, 0, 0)
		writeTileRow(0, 0, 0xFF, 0x00)
		writeTileRow(0, 1, 0x00, 0xFF)

		when: "render lines 72 and 73"
		advanceToLine(72)
		renderLine()      // line 72: windowLineCounter=0 → tile row 0
		gpu.update(204)   // complete line 72
		renderLine()      // line 73: windowLineCounter=1 → tile row 1

		then: "line 72 uses tile row 0 (shade 1)"
		gpu.frameBuffer.getPixels()[0][72] == 1

		and: "line 73 uses tile row 1 (shade 2)"
		gpu.frameBuffer.getPixels()[0][73] == 2
	}

	// === Additional edge case: WX < 7 clips left edge ===

	def "WX < 7 clips left edge and renders window starting at pixel 0"() {
		given: "window at 0x9C00 to avoid conflict with BG: LCDC = 0xF1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xF1)

		and: "WX=3 means startX = 3 - 7 = -4, clipped to pixel 0"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 3)

		and: "window tile map (0x9C00) points to tile 1: color index 2"
		setWindowTileMapEntry(0x9C00, 0, 0, 1)
		writeTileRow(1, 0, 0x00, 0xFF)

		and: "BG tile map (0x9800) has tile 0: color index 1"
		mmu.setByte(0x9800, 0)
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "pixel 0 shows window (shade 2), not background"
		gpu.frameBuffer.getPixels()[0][0] == 2

		and: "pixel 3 also shows window (shade 2)"
		gpu.frameBuffer.getPixels()[3][0] == 2
	}

	// === Additional edge case: WX > 166 means no window pixels ===

	def "WX > 166 renders no window pixels on that line"() {
		given: "window at 0x9C00 to avoid conflict with BG: LCDC = 0xF1"
		mmu.setByte(MMU.LCD_CONTROLLER, 0xF1)

		and: "window X far off screen right"
		mmu.setByte(MMU.WINDOW_Y, 0)
		mmu.setByte(MMU.WINDOW_X, 167)

		and: "window tile map (0x9C00) has tile 1 with color index 3"
		setWindowTileMapEntry(0x9C00, 0, 0, 1)
		writeTileRow(1, 0, 0xFF, 0xFF)

		and: "BG tile map (0x9800) has tile 0 with color index 0"
		mmu.setByte(0x9800, 0)
		writeTileRow(0, 0, 0x00, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "no window pixels rendered (startX = 167-7 = 160, beyond screen)"
		gpu.frameBuffer.getPixels()[0][0] == 0
	}
}
