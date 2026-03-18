package com.almejo.osom.gpu

import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.memory.MMU
import spock.lang.Specification

class SpriteRenderingSpec extends Specification {

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
		// Enable LCD + sprites: LCDC bit 7 + bit 1 = 0x82
		mmu.setByte(MMU.LCD_CONTROLLER, 0x82)
		// Set OBP0 to identity palette (0→0, 1→1, 2→2, 3→3)
		mmu.setByte(MMU.PALETTE_OBP0, 0xE4)
		// Allow OAM/VRAM test data writes (GPU internal mode stays SPRITES for renderLine)
		mmu.setStatModeBits(GPU.H_BLANK)
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
	 * Places a sprite in OAM at the given entry index (0-39).
	 * Y and X include the hardware offsets (Y+16, X+8 for screen position 0,0).
	 */
	private void placeSprite(int entry, int yPosition, int xPosition, int tileIndex, int attributes) {
		int oamAddress = 0xFE00 + entry * 4
		mmu.setByte(oamAddress + 0, yPosition)
		mmu.setByte(oamAddress + 1, xPosition)
		mmu.setByte(oamAddress + 2, tileIndex)
		mmu.setByte(oamAddress + 3, attributes)
	}

	/**
	 * Writes tile data (2 bytes per row) at the given tile index in the 0x8000 area.
	 */
	private void writeTileRow(int tileIndex, int row, int byte1, int byte2) {
		int address = 0x8000 + tileIndex * 16 + row * 2
		mmu.setByte(address, byte1)
		mmu.setByte(address + 1, byte2)
	}

	// === Task 4.1: Basic sprite rendering ===

	def "sprite renders on scanline at correct position"() {
		given: "a sprite at screen position (0, 0) using tile 0"
		placeSprite(0, 16, 8, 0, 0)

		and: "tile 0 row 0: byte1=0xFF, byte2=0x00 → all pixels have color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "pixels 0-7 on line 0 have shade 1 (from identity palette)"
		for (int x = 0; x < 8; x++) {
			gpu.frameBuffer.getPixels()[x][0] == 1
		}
	}

	def "sprite at screen position (10, 5) renders at correct offset"() {
		given: "a sprite at screen position (10, 5)"
		placeSprite(0, 5 + 16, 10 + 8, 0, 0)

		and: "tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 5"
		advanceToLine(5)
		renderLine()

		then: "pixels 10-17 on line 5 have shade 1"
		for (int x = 10; x < 18; x++) {
			gpu.frameBuffer.getPixels()[x][5] == 1
		}

		and: "pixel 9 (before sprite) is not affected"
		gpu.frameBuffer.getPixels()[9][5] == 0
	}

	def "sprite not on current scanline is not rendered"() {
		given: "a sprite at screen Y=10 (only covers lines 10-17 for 8x8)"
		placeSprite(0, 10 + 16, 8, 0, 0)

		and: "tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0 (sprite is at Y=10, not line 0)"
		renderLine()

		then: "no sprite pixels on line 0"
		for (int x = 0; x < 8; x++) {
			gpu.frameBuffer.getPixels()[x][0] == 0
		}
	}

	def "sprite tile data is read from 0x8000 unsigned area"() {
		given: "a sprite using tile index 2"
		placeSprite(0, 16, 8, 2, 0)

		and: "tile 2 at 0x8000 + 2*16 = 0x8020, row 0: color index 3"
		writeTileRow(2, 0, 0xFF, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "pixels have shade 3 (color index 3 through identity palette)"
		gpu.frameBuffer.getPixels()[0][0] == 3
	}

	def "sprite uses correct tile row based on scanline offset from sprite Y"() {
		given: "a sprite at screen Y=0, covering lines 0-7"
		placeSprite(0, 16, 8, 0, 0)

		and: "tile 0 row 3: byte1=0xFF, byte2=0x00 → color index 1"
		writeTileRow(0, 3, 0xFF, 0x00)

		when: "GPU renders line 3 (row 3 of the sprite)"
		advanceToLine(3)
		renderLine()

		then: "pixels on line 3 have shade 1 from tile row 3"
		gpu.frameBuffer.getPixels()[0][3] == 1
	}

	// === Task 4.2: Transparency ===

	def "sprite pixel with color index 0 is transparent and does not overwrite background"() {
		given: "background enabled with non-zero pixels"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93) // LCD + BG + sprites + tile data 0x8000
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)

		and: "BG tile 0 row 0: all pixels color index 2 (byte1=0x00, byte2=0xFF)"
		writeTileRow(0, 0, 0x00, 0xFF)
		mmu.setByte(0x9800, 0) // tile map points to tile 0

		and: "sprite at (0,0) using tile 1"
		placeSprite(0, 16, 8, 1, 0)

		and: "sprite tile 1 row 0: byte1=0x00, byte2=0x00 → all pixels color index 0 (transparent)"
		writeTileRow(1, 0, 0x00, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "background pixels are preserved (shade 2 from BG color index 2)"
		gpu.frameBuffer.getPixels()[0][0] == 2
	}

	def "sprite pixel with non-zero color index overwrites background"() {
		given: "background enabled"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93) // LCD + BG + sprites + tile data 0x8000
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)

		and: "BG tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)
		mmu.setByte(0x9800, 0) // tile map points to tile 0

		and: "sprite at (0,0) using tile 1 with color index 3"
		placeSprite(0, 16, 8, 1, 0)
		writeTileRow(1, 0, 0xFF, 0xFF) // color index 3

		when: "GPU renders line 0"
		renderLine()

		then: "sprite pixels (shade 3) overwrite background pixels (shade 1)"
		gpu.frameBuffer.getPixels()[0][0] == 3
	}

	// === Task 4.3: X-flip ===

	def "X-flip reverses horizontal pixel order"() {
		given: "a sprite with X-flip (attribute bit 5 = 0x20)"
		placeSprite(0, 16, 8, 0, 0x20)

		and: "tile 0 row 0: byte1=0x80, byte2=0x00 → only leftmost pixel (bit 7) has color index 1"
		writeTileRow(0, 0, 0x80, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "with X-flip, the leftmost pixel (bit 7) appears at rightmost screen position (x=7)"
		gpu.frameBuffer.getPixels()[7][0] == 1

		and: "the leftmost screen position (x=0) is transparent (was rightmost in original)"
		gpu.frameBuffer.getPixels()[0][0] == 0
	}

	def "without X-flip, leftmost tile pixel appears at leftmost screen position"() {
		given: "a sprite without X-flip"
		placeSprite(0, 16, 8, 0, 0)

		and: "tile 0 row 0: byte1=0x80, byte2=0x00 → only leftmost pixel (bit 7) has color index 1"
		writeTileRow(0, 0, 0x80, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "leftmost tile pixel appears at leftmost screen position (x=0)"
		gpu.frameBuffer.getPixels()[0][0] == 1

		and: "rightmost screen position (x=7) is transparent"
		gpu.frameBuffer.getPixels()[7][0] == 0
	}

	// === Task 4.4: Y-flip ===

	def "Y-flip inverts tile row selection for 8x8 sprite"() {
		given: "a sprite with Y-flip (attribute bit 6 = 0x40)"
		placeSprite(0, 16, 8, 0, 0x40)

		and: "tile 0 row 0 has no data (color index 0)"
		writeTileRow(0, 0, 0x00, 0x00)

		and: "tile 0 row 7 has data: all pixels color index 1"
		writeTileRow(0, 7, 0xFF, 0x00)

		when: "GPU renders line 0 (with Y-flip, row 7 should be displayed at line 0)"
		renderLine()

		then: "pixels have shade 1 from tile row 7 (Y-flipped)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	def "without Y-flip, line 0 uses tile row 0"() {
		given: "a sprite without Y-flip"
		placeSprite(0, 16, 8, 0, 0)

		and: "tile 0 row 0 has data: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		and: "tile 0 row 7 has no data"
		writeTileRow(0, 7, 0x00, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "pixels have shade 1 from tile row 0 (no flip)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	// === Task 4.5: BG priority ===

	def "sprite with BG priority is hidden behind non-zero background pixels"() {
		given: "background and sprites both enabled"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93) // LCD + BG + sprites + tile data 0x8000
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)

		and: "BG tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)
		mmu.setByte(0x9800, 0) // tile map points to tile 0

		and: "sprite at (0,0) using tile 1 with BG priority (bit 7 = 0x80)"
		placeSprite(0, 16, 8, 1, 0x80)

		and: "sprite tile 1 row 0: all pixels color index 3"
		writeTileRow(1, 0, 0xFF, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "background pixels (shade 1) take priority over sprite (shade 3)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	def "sprite with BG priority renders over background color index 0"() {
		given: "background and sprites both enabled"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)

		and: "BG tile 0 row 0: all pixels color index 0 (transparent BG)"
		writeTileRow(0, 0, 0x00, 0x00)
		mmu.setByte(0x9800, 0)

		and: "sprite at (0,0) with BG priority"
		placeSprite(0, 16, 8, 1, 0x80)
		writeTileRow(1, 0, 0xFF, 0xFF) // color index 3

		when: "GPU renders line 0"
		renderLine()

		then: "sprite renders because BG color index is 0"
		gpu.frameBuffer.getPixels()[0][0] == 3
	}

	// === Task 4.6: OBP0/OBP1 palette selection ===

	def "sprite uses OBP0 palette when attribute bit 4 is 0"() {
		given: "OBP0 set to reversed palette, OBP1 set to identity"
		mmu.setByte(MMU.PALETTE_OBP0, 0x1B) // reversed: 0→3, 1→2, 2→1, 3→0
		mmu.setByte(MMU.PALETTE_OBP1, 0xE4) // identity

		and: "sprite with attribute bit 4 = 0 (use OBP0)"
		placeSprite(0, 16, 8, 0, 0x00)
		writeTileRow(0, 0, 0xFF, 0x00) // color index 1

		when: "GPU renders line 0"
		renderLine()

		then: "shade is from OBP0: color index 1 → shade 2 (reversed palette)"
		gpu.frameBuffer.getPixels()[0][0] == 2
	}

	def "sprite uses OBP1 palette when attribute bit 4 is 1"() {
		given: "OBP0 set to reversed palette, OBP1 set to identity"
		mmu.setByte(MMU.PALETTE_OBP0, 0x1B) // reversed
		mmu.setByte(MMU.PALETTE_OBP1, 0xE4) // identity

		and: "sprite with attribute bit 4 = 1 (0x10) (use OBP1)"
		placeSprite(0, 16, 8, 0, 0x10)
		writeTileRow(0, 0, 0xFF, 0x00) // color index 1

		when: "GPU renders line 0"
		renderLine()

		then: "shade is from OBP1: color index 1 → shade 1 (identity palette)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	// === Task 4.7: 8x16 sprite mode ===

	def "8x16 sprite renders top tile with LSB cleared"() {
		given: "LCDC bit 2 set for 8x16 sprites"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x86) // LCD + sprites + 8x16 mode

		and: "sprite at screen Y=0 using tile index 0x03"
		placeSprite(0, 16, 8, 0x03, 0)

		and: "top tile (index 0x02 = 0x03 & 0xFE) row 0: color index 1"
		writeTileRow(0x02, 0, 0xFF, 0x00)

		when: "GPU renders line 0 (top tile, row 0)"
		renderLine()

		then: "pixels have shade 1 from top tile (0x02)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	def "8x16 sprite renders bottom tile with LSB set"() {
		given: "LCDC bit 2 set for 8x16 sprites"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x86) // LCD + sprites + 8x16 mode

		and: "sprite at screen Y=0 using tile index 0x02"
		placeSprite(0, 16, 8, 0x02, 0)

		and: "bottom tile (index 0x03 = 0x02 | 0x01) row 0: color index 2"
		writeTileRow(0x03, 0, 0x00, 0xFF)

		when: "GPU renders line 8 (bottom tile, row 0)"
		advanceToLine(8)
		renderLine()

		then: "pixels have shade 2 from bottom tile (0x03)"
		gpu.frameBuffer.getPixels()[0][8] == 2
	}

	def "8x16 sprite covers 16 scanlines"() {
		given: "LCDC bit 2 set for 8x16 sprites"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x86)

		and: "sprite at screen Y=0"
		placeSprite(0, 16, 8, 0x00, 0)

		and: "top tile (0x00) all rows: color index 1"
		for (int row = 0; row < 8; row++) {
			writeTileRow(0x00, row, 0xFF, 0x00)
		}

		and: "bottom tile (0x01) all rows: color index 1"
		for (int row = 0; row < 8; row++) {
			writeTileRow(0x01, row, 0xFF, 0x00)
		}

		when: "GPU renders line 15 (last row of 8x16 sprite)"
		advanceToLine(15)
		renderLine()

		then: "pixels still rendered on line 15"
		gpu.frameBuffer.getPixels()[0][15] == 1
	}

	def "8x16 sprite does not render on line 16"() {
		given: "LCDC bit 2 set for 8x16 sprites"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x86)

		and: "sprite at screen Y=0"
		placeSprite(0, 16, 8, 0x00, 0)

		and: "tile data with color index 1"
		for (int row = 0; row < 8; row++) {
			writeTileRow(0x00, row, 0xFF, 0x00)
			writeTileRow(0x01, row, 0xFF, 0x00)
		}

		when: "GPU renders line 16 (beyond 8x16 sprite height)"
		advanceToLine(16)
		renderLine()

		then: "no sprite pixels on line 16"
		gpu.frameBuffer.getPixels()[0][16] == 0
	}

	// === Task 4.8: 10-sprite-per-scanline limit ===

	def "only first 10 sprites per scanline are rendered"() {
		given: "11 sprites all on line 0 at different X positions"
		for (int sprite = 0; sprite < 11; sprite++) {
			placeSprite(sprite, 16, 8 + sprite * 8, 0, 0)
		}

		and: "tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "first 10 sprites are rendered (sprites 0-9 at X positions 0-79)"
		gpu.frameBuffer.getPixels()[0][0] == 1   // sprite 0
		gpu.frameBuffer.getPixels()[72][0] == 1  // sprite 9

		and: "11th sprite (sprite 10, starting at X=80) is NOT rendered"
		gpu.frameBuffer.getPixels()[80][0] == 0
	}

	// === Task 4.9: Sprite at screen edge ===

	def "sprite partially off left edge renders only visible pixels"() {
		given: "a sprite at X=4 (screen X starts at -4, so pixels 0-3 are off-screen)"
		placeSprite(0, 16, 4, 0, 0)

		and: "tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "only pixels 0-3 are rendered (the right half of the sprite)"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[3][0] == 1
	}

	def "sprite partially off right edge renders only visible pixels"() {
		given: "a sprite at X=164 (screen X starts at 156, so pixels 156-159 visible, 160-163 off-screen)"
		placeSprite(0, 16, 164, 0, 0)

		and: "tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "pixels 156-159 are rendered"
		gpu.frameBuffer.getPixels()[156][0] == 1
		gpu.frameBuffer.getPixels()[159][0] == 1
	}

	def "sprite completely off screen left (X=0) renders nothing"() {
		given: "a sprite at X=0 (screen X starts at -8, all pixels off-screen)"
		placeSprite(0, 16, 0, 0, 0)

		and: "tile 0 row 0: all pixels color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "GPU renders line 0"
		renderLine()

		then: "no pixels rendered"
		gpu.frameBuffer.getPixels()[0][0] == 0
	}

	def "multiple sprites render correctly with different tiles"() {
		given: "two sprites at different positions with different tiles"
		placeSprite(0, 16, 8, 0, 0)       // sprite 0 at screen (0,0), tile 0
		placeSprite(1, 16, 16, 1, 0)      // sprite 1 at screen (8,0), tile 1

		and: "tile 0: color index 1, tile 1: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00) // color index 1
		writeTileRow(1, 0, 0x00, 0xFF) // color index 2

		when: "GPU renders line 0"
		renderLine()

		then: "sprite 0 pixels (0-7) have shade 1"
		gpu.frameBuffer.getPixels()[0][0] == 1

		and: "sprite 1 pixels (8-15) have shade 2"
		gpu.frameBuffer.getPixels()[8][0] == 2
	}

	// === Combined attribute tests ===

	def "X-flip and Y-flip combined"() {
		given: "a sprite with both X-flip and Y-flip (0x60)"
		placeSprite(0, 16, 8, 0, 0x60)

		and: "tile 0 row 7: only leftmost pixel (bit 7) has color index 1"
		writeTileRow(0, 7, 0x80, 0x00)

		when: "GPU renders line 0 (Y-flip: row 7 shown at line 0, X-flip: bit 7 at rightmost)"
		renderLine()

		then: "pixel appears at x=7 (X-flipped) from row 7 (Y-flipped)"
		gpu.frameBuffer.getPixels()[7][0] == 1

		and: "pixel at x=0 is transparent"
		gpu.frameBuffer.getPixels()[0][0] == 0
	}
}
