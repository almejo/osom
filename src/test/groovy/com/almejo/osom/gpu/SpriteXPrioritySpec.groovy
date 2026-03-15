package com.almejo.osom.gpu

import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.memory.MMU
import spock.lang.Specification

class SpriteXPrioritySpec extends Specification {

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
	}

	private void renderLine() {
		gpu.update(80)
		gpu.update(172)
	}

	private void advanceToLine(int targetLine) {
		for (int scanline = 0; scanline < targetLine; scanline++) {
			gpu.update(80)
			gpu.update(172)
			gpu.update(204)
		}
	}

	private void placeSprite(int entry, int yPosition, int xPosition, int tileIndex, int attributes) {
		int oamAddress = 0xFE00 + entry * 4
		mmu.setByte(oamAddress + 0, yPosition)
		mmu.setByte(oamAddress + 1, xPosition)
		mmu.setByte(oamAddress + 2, tileIndex)
		mmu.setByte(oamAddress + 3, attributes)
	}

	private void writeTileRow(int tileIndex, int row, int byte1, int byte2) {
		int address = 0x8000 + tileIndex * 16 + row * 2
		mmu.setByte(address, byte1)
		mmu.setByte(address + 1, byte2)
	}

	// === Task 4.1: Two overlapping sprites — lower X wins ===

	def "sprite with lower X coordinate renders on top when overlapping"() {
		given: "sprite 0 at OAM 5 with screen X=12, sprite 1 at OAM 2 with screen X=8"
		placeSprite(5, 16, 12 + 8, 0, 0)  // OAM 5, screen X=12, tile 0 (covers pixels 12-19)
		placeSprite(2, 16, 8 + 8, 1, 0)   // OAM 2, screen X=8, tile 1 (covers pixels 8-15)

		and: "tile 0: color index 1, tile 1: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00)  // color index 1
		writeTileRow(1, 0, 0x00, 0xFF)  // color index 2

		when: "GPU renders line 0"
		renderLine()

		then: "in overlap zone (pixels 12-15), sprite 1 (X=8, lower X) wins with shade 2"
		gpu.frameBuffer.getPixels()[12][0] == 2
		gpu.frameBuffer.getPixels()[13][0] == 2
		gpu.frameBuffer.getPixels()[14][0] == 2
		gpu.frameBuffer.getPixels()[15][0] == 2

		and: "non-overlapping zone of sprite 1 (pixels 8-11) shows shade 2"
		gpu.frameBuffer.getPixels()[8][0] == 2
		gpu.frameBuffer.getPixels()[11][0] == 2

		and: "non-overlapping zone of sprite 0 (pixels 16-19) shows shade 1"
		gpu.frameBuffer.getPixels()[16][0] == 1
		gpu.frameBuffer.getPixels()[19][0] == 1
	}

	def "two sprites at same position — lower X wins regardless of OAM order"() {
		given: "OAM 10 at screen X=0, OAM 0 at screen X=16 — both fully overlap at pixels 0-7"
		placeSprite(10, 16, 8, 0, 0)  // OAM 10, screen X=0, tile 0
		placeSprite(0, 16, 24, 1, 0)  // OAM 0, screen X=16, tile 1

		and: "tile 0: color index 1, tile 1: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00)
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "sprite at X=0 (OAM 10, higher OAM but lower X) wins at pixels 0-7"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[7][0] == 1
	}

	// === Task 4.2: Two overlapping sprites same X — lower OAM index wins ===

	def "sprite with lower OAM index wins when both at same X"() {
		given: "sprite at OAM 0 and OAM 5, both at screen X=0, same Y, different tiles"
		placeSprite(0, 16, 8, 0, 0)  // OAM 0, screen X=0, tile 0
		placeSprite(5, 16, 8, 1, 0)  // OAM 5, screen X=0, tile 1

		and: "tile 0: color index 1, tile 1: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00)
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "OAM 0 wins — pixel has shade 1 (from tile 0)"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[7][0] == 1
	}

	def "higher OAM index loses when both at same X"() {
		given: "sprite at OAM 3 and OAM 7 both at screen X=0"
		placeSprite(3, 16, 8, 0, 0)  // OAM 3, tile 0
		placeSprite(7, 16, 8, 1, 0)  // OAM 7, tile 1

		and: "tile 0: color index 3, tile 1: color index 1"
		writeTileRow(0, 0, 0xFF, 0xFF)  // color index 3
		writeTileRow(1, 0, 0xFF, 0x00)  // color index 1

		when: "GPU renders line 0"
		renderLine()

		then: "OAM 3 (lower index) wins with shade 3"
		gpu.frameBuffer.getPixels()[0][0] == 3
	}

	// === Task 4.3: High-priority sprite transparent pixel shows lower-priority sprite ===

	def "transparent pixel of higher-priority sprite shows lower-priority sprite"() {
		given: "two sprites at same X — OAM 0 has transparent pixels, OAM 1 has opaque"
		placeSprite(0, 16, 8, 0, 0)  // OAM 0, screen X=0, tile 0 (higher priority)
		placeSprite(1, 16, 8, 1, 0)  // OAM 1, screen X=0, tile 1

		and: "tile 0: all pixels color index 0 (transparent)"
		writeTileRow(0, 0, 0x00, 0x00)

		and: "tile 1: all pixels color index 2"
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "lower-priority sprite (OAM 1) shows through transparent pixels of higher-priority sprite"
		gpu.frameBuffer.getPixels()[0][0] == 2
		gpu.frameBuffer.getPixels()[7][0] == 2
	}

	def "partial transparency — high-priority sprite has mix of transparent and opaque"() {
		given: "two sprites at same X"
		placeSprite(0, 16, 8, 0, 0)  // OAM 0, higher priority
		placeSprite(1, 16, 8, 1, 0)  // OAM 1, lower priority

		and: "tile 0: left 4 pixels color index 1, right 4 pixels transparent (color 0)"
		writeTileRow(0, 0, 0xF0, 0x00)  // bits 7-4 set = left 4 pixels = color index 1

		and: "tile 1: all pixels color index 2"
		writeTileRow(1, 0, 0x00, 0xFF)  // color index 2

		when: "GPU renders line 0"
		renderLine()

		then: "left 4 pixels show higher-priority sprite (shade 1)"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[3][0] == 1

		and: "right 4 pixels show lower-priority sprite through transparency (shade 2)"
		gpu.frameBuffer.getPixels()[4][0] == 2
		gpu.frameBuffer.getPixels()[7][0] == 2
	}

	// === Task 4.4: Three overlapping sprites with different X — correct layering ===

	def "three overlapping sprites layered correctly by X coordinate"() {
		given: "three sprites overlapping at screen X=0, with different X values"
		placeSprite(0, 16, 8, 0, 0)      // OAM 0, screen X=0, tile 0
		placeSprite(1, 16, 8 + 4, 1, 0)  // OAM 1, screen X=4, tile 1
		placeSprite(2, 16, 8 + 2, 2, 0)  // OAM 2, screen X=2, tile 2

		and: "tile 0: color index 1, tile 1: color index 2, tile 2: color index 3"
		writeTileRow(0, 0, 0xFF, 0x00)  // color index 1
		writeTileRow(1, 0, 0x00, 0xFF)  // color index 2
		writeTileRow(2, 0, 0xFF, 0xFF)  // color index 3

		when: "GPU renders line 0"
		renderLine()

		then: "pixel 0-1: only sprite 0 (X=0) covers → shade 1"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[1][0] == 1

		and: "pixel 2-3: sprites 0 (X=0) and 2 (X=2) overlap → sprite 0 (lower X) wins → shade 1"
		gpu.frameBuffer.getPixels()[2][0] == 1
		gpu.frameBuffer.getPixels()[3][0] == 1

		and: "pixel 4-7: all three overlap → sprite 0 (X=0, lowest) wins → shade 1"
		gpu.frameBuffer.getPixels()[4][0] == 1
		gpu.frameBuffer.getPixels()[7][0] == 1

		and: "pixel 8-9: sprites 2 (X=2) and 1 (X=4) overlap → sprite 2 (lower X) wins → shade 3"
		gpu.frameBuffer.getPixels()[8][0] == 3
		gpu.frameBuffer.getPixels()[9][0] == 3

		and: "pixel 10-11: only sprite 1 (X=4) covers → shade 2"
		gpu.frameBuffer.getPixels()[10][0] == 2
		gpu.frameBuffer.getPixels()[11][0] == 2
	}

	// === Task 4.5: X-priority with BG priority flag ===

	def "X-priority sorting does not affect BG priority evaluation per-sprite"() {
		given: "BG enabled with non-zero background"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93) // LCD + BG + sprites + tile data 0x8000
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)

		and: "BG tile 0: color index 1 (non-zero)"
		writeTileRow(0, 0, 0xFF, 0x00)
		mmu.setByte(0x9800, 0)

		and: "sprite at OAM 0 with higher X, NO BG priority flag"
		placeSprite(0, 16, 16, 1, 0x00)  // OAM 0, screen X=8, tile 1, no BG priority

		and: "sprite at OAM 1 with lower X, WITH BG priority flag"
		placeSprite(1, 16, 8, 2, 0x80)  // OAM 1, screen X=0, tile 2, BG priority set

		and: "tile 1: color index 2, tile 2: color index 3"
		writeTileRow(1, 0, 0x00, 0xFF)  // color index 2
		writeTileRow(2, 0, 0xFF, 0xFF)  // color index 3

		when: "GPU renders line 0"
		renderLine()

		then: "at pixels 0-7 (sprite OAM 1, X=0, BG priority): background wins (shade 1)"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[7][0] == 1

		and: "at pixels 8-15 (sprite OAM 0, X=8, no BG priority): sprite wins (shade 2)"
		gpu.frameBuffer.getPixels()[8][0] == 2
		gpu.frameBuffer.getPixels()[15][0] == 2
	}

	def "BG priority evaluated per-sprite independently even with X overlap"() {
		given: "BG enabled"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)

		and: "BG with color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)
		mmu.setByte(0x9800, 0)

		and: "sprite OAM 0 at X=0, BG priority flag SET"
		placeSprite(0, 16, 8, 1, 0x80)  // BG priority

		and: "sprite OAM 1 at same X=0, NO BG priority flag"
		placeSprite(1, 16, 8, 2, 0x00)  // no BG priority

		and: "tile 1: color index 3, tile 2: color index 2"
		writeTileRow(1, 0, 0xFF, 0xFF)  // color index 3
		writeTileRow(2, 0, 0x00, 0xFF)  // color index 2

		when: "GPU renders line 0"
		renderLine()

		then: "OAM 0 wins X-priority (lower OAM at same X), but has BG priority → BG shows"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	// === Task 4.6: Non-overlapping sprites unaffected by sorting ===

	def "non-overlapping sprites are unaffected by X-priority sorting"() {
		given: "two non-overlapping sprites at different X positions"
		placeSprite(5, 16, 80 + 8, 0, 0)  // OAM 5, screen X=80, tile 0
		placeSprite(2, 16, 8, 1, 0)        // OAM 2, screen X=0, tile 1

		and: "tile 0: color index 1, tile 1: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00)
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "sprite at X=0 renders at pixels 0-7 with shade 2"
		gpu.frameBuffer.getPixels()[0][0] == 2
		gpu.frameBuffer.getPixels()[7][0] == 2

		and: "sprite at X=80 renders at pixels 80-87 with shade 1"
		gpu.frameBuffer.getPixels()[80][0] == 1
		gpu.frameBuffer.getPixels()[87][0] == 1

		and: "gap between sprites is empty"
		gpu.frameBuffer.getPixels()[8][0] == 0
		gpu.frameBuffer.getPixels()[79][0] == 0
	}

	// === Task 4.7: 10-sprite limit still selects by OAM order, then sorts by X ===

	def "10-sprite limit selects by OAM scan order then sorts selected sprites by X"() {
		given: "11 sprites on line 0 — OAM 0-10, with OAM 10 at lowest X"
		for (int sprite = 0; sprite < 10; sprite++) {
			placeSprite(sprite, 16, 8 + (10 - sprite) * 8, 0, 0)  // OAM 0-9, decreasing X
		}
		placeSprite(10, 16, 8, 1, 0)  // OAM 10, screen X=0, tile 1 — LOWEST X but 11th sprite

		and: "tile 0: color index 1, tile 1: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00)
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "OAM 10 (11th sprite) is NOT selected — pixel at X=0 has no sprite"
		gpu.frameBuffer.getPixels()[0][0] == 0

		and: "OAM 9 (screen X=8) is selected and rendered"
		gpu.frameBuffer.getPixels()[8][0] == 1
	}

	// === Task 4.8: X-priority with X-flipped sprite ===

	def "X-flip affects pixel rendering but not priority X value"() {
		given: "two sprites at same X, OAM 0 with X-flip, OAM 1 without"
		placeSprite(0, 16, 8, 0, 0x20)  // OAM 0, screen X=0, tile 0, X-flip
		placeSprite(1, 16, 8, 1, 0x00)  // OAM 1, screen X=0, tile 1, no flip

		and: "tile 0: only leftmost pixel (bit 7) color index 1, rest transparent"
		writeTileRow(0, 0, 0x80, 0x00)

		and: "tile 1: all pixels color index 2"
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "OAM 0 wins (lower OAM at same X). X-flip moves its pixel to rightmost position"
		gpu.frameBuffer.getPixels()[7][0] == 1

		and: "OAM 0's transparent pixels (0-6) show OAM 1 underneath"
		gpu.frameBuffer.getPixels()[0][0] == 2
		gpu.frameBuffer.getPixels()[6][0] == 2
	}

	// === Task 4.9: X-priority with 8x16 sprites ===

	def "8x16 sprites sorted by same X priority rule"() {
		given: "8x16 mode enabled"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x86) // LCD + sprites + 8x16

		and: "two 8x16 sprites overlapping — OAM 0 at X=4, OAM 1 at X=0"
		placeSprite(0, 16, 8 + 4, 0x02, 0)  // OAM 0, screen X=4, tile 0x02
		placeSprite(1, 16, 8, 0x04, 0)       // OAM 1, screen X=0, tile 0x04

		and: "tile 0x02 (top of OAM 0): color index 1"
		writeTileRow(0x02, 0, 0xFF, 0x00)

		and: "tile 0x04 (top of OAM 1): color index 2"
		writeTileRow(0x04, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "overlap at pixels 4-7: OAM 1 (X=0, lower X) wins with shade 2"
		gpu.frameBuffer.getPixels()[4][0] == 2
		gpu.frameBuffer.getPixels()[7][0] == 2

		and: "non-overlap pixels 0-3: only OAM 1 → shade 2"
		gpu.frameBuffer.getPixels()[0][0] == 2
		gpu.frameBuffer.getPixels()[3][0] == 2

		and: "non-overlap pixels 8-11: only OAM 0 → shade 1"
		gpu.frameBuffer.getPixels()[8][0] == 1
		gpu.frameBuffer.getPixels()[11][0] == 1
	}

	// === Task 4.10: All 10 sprites at same X — pure OAM index tiebreaker ===

	def "all 10 sprites at same X uses OAM index as tiebreaker"() {
		given: "10 sprites all at screen X=0 with different tiles"
		for (int sprite = 0; sprite < 10; sprite++) {
			placeSprite(sprite, 16, 8, sprite, 0)  // OAM 0-9, all at X=8 (screen X=0), tiles 0-9
		}

		and: "each tile has a different color index for identification"
		writeTileRow(0, 0, 0xFF, 0x00)  // tile 0: color index 1
		writeTileRow(1, 0, 0x00, 0xFF)  // tile 1: color index 2
		writeTileRow(2, 0, 0xFF, 0xFF)  // tile 2: color index 3
		for (int tile = 3; tile < 10; tile++) {
			writeTileRow(tile, 0, 0xFF, 0x00)  // tiles 3-9: color index 1
		}

		when: "GPU renders line 0"
		renderLine()

		then: "OAM 0 (lowest index) wins — shade 1 (from tile 0, color index 1)"
		gpu.frameBuffer.getPixels()[0][0] == 1
		gpu.frameBuffer.getPixels()[7][0] == 1
	}

	def "OAM index 9 loses to OAM index 0 at same X"() {
		given: "only two sprites — OAM 9 placed first in test, OAM 0 placed second, same X"
		placeSprite(9, 16, 8, 1, 0)  // OAM 9, screen X=0, tile 1
		placeSprite(0, 16, 8, 0, 0)  // OAM 0, screen X=0, tile 0

		and: "tile 0: color index 1, tile 1: color index 2"
		writeTileRow(0, 0, 0xFF, 0x00)
		writeTileRow(1, 0, 0x00, 0xFF)

		when: "GPU renders line 0"
		renderLine()

		then: "OAM 0 wins (lower index) — shade 1"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	// === Additional edge cases ===

	def "collectVisibleSprites returns array with correct length"() {
		given: "3 sprites on scanline, 1 off-screen"
		placeSprite(0, 16, 8, 0, 0)     // on scanline 0
		placeSprite(1, 16, 16, 0, 0)    // on scanline 0
		placeSprite(2, 50, 8, 0, 0)     // Y=50-16=34, not on scanline 0
		placeSprite(3, 16, 24, 0, 0)    // on scanline 0

		when: "collecting visible sprites for 8x8 mode"
		int[][] result = gpu.collectVisibleSprites(8)

		then: "3 sprites visible on scanline 0"
		result.length == 3
	}

	def "collectVisibleSprites respects 10-sprite limit"() {
		given: "15 sprites all on scanline 0"
		for (int sprite = 0; sprite < 15; sprite++) {
			placeSprite(sprite, 16, 8 + sprite * 8, 0, 0)
		}

		when: "collecting visible sprites"
		int[][] result = gpu.collectVisibleSprites(8)

		then: "only 10 sprites collected"
		result.length == 10
	}

	def "renderSingleSprite renders sprite data correctly and claims pixels"() {
		given: "sprite data array: Y=0, X=8 (screen X=0), tile=0, attributes=0"
		int[] spriteData = [0, 8, 0, 0]
		boolean[] pixelClaimed = new boolean[160]

		and: "tile 0 row 0: color index 1"
		writeTileRow(0, 0, 0xFF, 0x00)

		when: "rendering single sprite at line 0"
		gpu.renderSingleSprite(spriteData, 8, pixelClaimed)

		then: "pixels 0-7 have shade 1"
		for (int x = 0; x < 8; x++) {
			assert gpu.frameBuffer.getPixels()[x][0] == 1
		}

		and: "pixels 0-7 are claimed"
		for (int x = 0; x < 8; x++) {
			assert pixelClaimed[x]
		}

		and: "pixel 8 is not claimed"
		!pixelClaimed[8]
	}
}
