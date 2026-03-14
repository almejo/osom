package com.almejo.osom.gpu

import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.memory.MMU
import spock.lang.Specification

class GPUSpec extends Specification {

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

	// === getSpriteHeight ===

	def "getSpriteHeight returns 8 when LCDC bit 2 is clear"() {
		expect:
		gpu.getSpriteHeight(0x00) == 8
	}

	def "getSpriteHeight returns 16 when LCDC bit 2 is set"() {
		expect:
		gpu.getSpriteHeight(0x04) == 16
	}

	// === isSpriteOnScanline ===

	def "isSpriteOnScanline returns true when line is within sprite range"() {
		given: "GPU is on line 5"
		advanceToLine(5)

		expect: "sprite at Y=3 with height 8 covers lines 3-10"
		gpu.isSpriteOnScanline(3, 8)
	}

	def "isSpriteOnScanline returns false when line is above sprite"() {
		given: "GPU is on line 0"
		// GPU starts at line 0

		expect: "sprite at Y=5 is below current line"
		!gpu.isSpriteOnScanline(5, 8)
	}

	def "isSpriteOnScanline returns false when line is below sprite"() {
		given: "GPU is on line 10"
		advanceToLine(10)

		expect: "sprite at Y=0 with height 8 covers lines 0-7, not 10"
		!gpu.isSpriteOnScanline(0, 8)
	}

	def "isSpriteOnScanline returns true at exact top boundary"() {
		given: "GPU is on line 5"
		advanceToLine(5)

		expect: "sprite at Y=5 with height 8 starts exactly at line 5"
		gpu.isSpriteOnScanline(5, 8)
	}

	def "isSpriteOnScanline returns false at exact bottom boundary"() {
		given: "GPU is on line 13"
		advanceToLine(13)

		expect: "sprite at Y=5 with height 8 covers lines 5-12, not 13"
		!gpu.isSpriteOnScanline(5, 8)
	}

	// === isPixelOffScreen ===

	def "isPixelOffScreen returns true for negative X"() {
		expect:
		gpu.isPixelOffScreen(-1)
	}

	def "isPixelOffScreen returns false for X=0"() {
		expect:
		!gpu.isPixelOffScreen(0)
	}

	def "isPixelOffScreen returns false for X in middle of screen"() {
		expect:
		!gpu.isPixelOffScreen(80)
	}

	def "isPixelOffScreen returns false for X=159 (last visible pixel)"() {
		expect:
		!gpu.isPixelOffScreen(159)
	}

	def "isPixelOffScreen returns true for X=160 (just past screen edge)"() {
		expect:
		gpu.isPixelOffScreen(160)
	}

	// === isHiddenBehindBackground ===

	def "isHiddenBehindBackground returns false when bgPriority is false"() {
		given: "render a line with non-zero background"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)
		writeTileRow(0, 0, 0xFF, 0x00)
		mmu.setByte(0x9800, 0)
		renderLineForSetup()

		expect: "bgPriority false means sprite is always visible"
		!gpu.isHiddenBehindBackground(false, 0)
	}

	def "isHiddenBehindBackground returns false when bgPriority is true but BG color index is 0"() {
		given: "render a line with zero background"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)
		writeTileRow(0, 0, 0x00, 0x00)
		mmu.setByte(0x9800, 0)
		renderLineForSetup()

		expect: "bgPriority true but BG color index 0 means sprite is visible"
		!gpu.isHiddenBehindBackground(true, 0)
	}

	def "isHiddenBehindBackground returns true when bgPriority is true and BG color index is non-zero"() {
		given: "render a line with non-zero background"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)
		writeTileRow(0, 0, 0xFF, 0x00)
		mmu.setByte(0x9800, 0)
		renderLineForSetup()

		expect: "bgPriority true and BG color index non-zero means sprite is hidden"
		gpu.isHiddenBehindBackground(true, 0)
	}

	// === resolveSpriteTileRow ===

	def "resolveSpriteTileRow returns tileRow unchanged without flip"() {
		expect:
		gpu.resolveSpriteTileRow(3, 8, false) == 3
	}

	def "resolveSpriteTileRow flips row for 8x8 sprite with yFlip"() {
		expect: "row 0 flipped in 8-height sprite becomes row 7"
		gpu.resolveSpriteTileRow(0, 8, true) == 7
	}

	def "resolveSpriteTileRow flips row for 8x16 sprite with yFlip"() {
		expect: "row 2 flipped in 16-height sprite becomes row 13"
		gpu.resolveSpriteTileRow(2, 16, true) == 13
	}

	// === adjustTileIndexForDoubleHeight ===

	def "adjustTileIndexForDoubleHeight clears LSB for top tile in 8x16 mode"() {
		when:
		int[] result = gpu.adjustTileIndexForDoubleHeight(0x03, 5, 16)

		then: "tile index has LSB cleared, row unchanged"
		result[0] == 0x02
		result[1] == 5
	}

	def "adjustTileIndexForDoubleHeight sets LSB and adjusts row for bottom tile in 8x16 mode"() {
		when:
		int[] result = gpu.adjustTileIndexForDoubleHeight(0x02, 10, 16)

		then: "tile index has LSB set, row reduced by 8"
		result[0] == 0x03
		result[1] == 2
	}

	def "adjustTileIndexForDoubleHeight does not modify values in 8x8 mode"() {
		when:
		int[] result = gpu.adjustTileIndexForDoubleHeight(0x05, 3, 8)

		then: "both values unchanged"
		result[0] == 0x05
		result[1] == 3
	}

	// === decodeTileColorIndex ===

	def "decodeTileColorIndex returns 0 when both bits are clear"() {
		expect: "bit 7: byte1=0x00 (bit7=0), byte2=0x00 (bit7=0) → color 0"
		gpu.decodeTileColorIndex(0x00, 0x00, 7) == 0
	}

	def "decodeTileColorIndex returns 1 when only byte1 bit is set"() {
		expect: "bit 7: byte1=0x80 (bit7=1), byte2=0x00 (bit7=0) → color 1"
		gpu.decodeTileColorIndex(0x80, 0x00, 7) == 1
	}

	def "decodeTileColorIndex returns 2 when only byte2 bit is set"() {
		expect: "bit 7: byte1=0x00 (bit7=0), byte2=0x80 (bit7=1) → color 2"
		gpu.decodeTileColorIndex(0x00, 0x80, 7) == 2
	}

	def "decodeTileColorIndex returns 3 when both bits are set"() {
		expect: "bit 7: byte1=0x80 (bit7=1), byte2=0x80 (bit7=1) → color 3"
		gpu.decodeTileColorIndex(0x80, 0x80, 7) == 3
	}

	// === renderSpritePixel ===

	def "renderSpritePixel does not write when colorIndex is 0 (transparent)"() {
		given: "framebuffer pixel is initially 0"
		gpu.renderSpritePixel(0, 0, false, false)

		expect: "pixel unchanged"
		gpu.frameBuffer.getPixels()[0][0] == 0
	}

	def "renderSpritePixel writes shade to framebuffer for non-zero colorIndex"() {
		when:
		gpu.renderSpritePixel(5, 1, false, false)

		then: "pixel at (5, 0) has shade 1 from OBP0 identity palette"
		gpu.frameBuffer.getPixels()[5][0] == 1
	}

	def "renderSpritePixel does not write when hidden behind background"() {
		given: "render a line with non-zero background at pixel 0"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x93)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)
		writeTileRow(0, 0, 0xFF, 0x00)
		mmu.setByte(0x9800, 0)
		renderLineForSetup()

		when: "sprite tries to render with BG priority at pixel 0"
		gpu.renderSpritePixel(0, 3, true, false)

		then: "pixel keeps background shade (1), not sprite shade (3)"
		gpu.frameBuffer.getPixels()[0][0] == 1
	}

	def "renderSpritePixel uses OBP1 palette when usePalette1 is true"() {
		given: "OBP1 set to reversed palette"
		mmu.setByte(MMU.PALETTE_OBP1, 0x1B)

		when: "render with usePalette1=true, color index 1"
		gpu.renderSpritePixel(10, 1, false, true)

		then: "shade is from OBP1 reversed palette: color index 1 → shade 2"
		gpu.frameBuffer.getPixels()[10][0] == 2
	}

	// === getTileDataBase ===

	def "getTileDataBase returns 0x8000 when LCDC bit 4 is set"() {
		expect: "bit 4 set (0x10) selects unsigned tile data base"
		gpu.getTileDataBase(0x10) == 0x8000
	}

	def "getTileDataBase returns 0x8800 when LCDC bit 4 is clear"() {
		expect: "bit 4 clear selects signed tile data base"
		gpu.getTileDataBase(0x00) == 0x8800
	}

	// === useUnsignedTileAddressing ===

	def "useUnsignedTileAddressing returns true when LCDC bit 4 is set"() {
		expect:
		gpu.useUnsignedTileAddressing(0x10)
	}

	def "useUnsignedTileAddressing returns false when LCDC bit 4 is clear"() {
		expect:
		!gpu.useUnsignedTileAddressing(0x00)
	}

	// === getBackgroundTileMapBase ===

	def "getBackgroundTileMapBase returns 0x9800 when LCDC bit 3 is clear"() {
		expect:
		gpu.getBackgroundTileMapBase(0x00) == 0x9800
	}

	def "getBackgroundTileMapBase returns 0x9C00 when LCDC bit 3 is set"() {
		expect:
		gpu.getBackgroundTileMapBase(0x08) == 0x9C00
	}

	// === getWindowTileMapBase ===

	def "getWindowTileMapBase returns 0x9800 when LCDC bit 6 is clear"() {
		expect:
		gpu.getWindowTileMapBase(0x00) == 0x9800
	}

	def "getWindowTileMapBase returns 0x9C00 when LCDC bit 6 is set"() {
		expect:
		gpu.getWindowTileMapBase(0x40) == 0x9C00
	}

	// === readTileIndex ===

	def "readTileIndex reads unsigned byte when useUnsigned is true"() {
		given: "tile map at 0x9800 has value 200 at column 0, row 0"
		mmu.setByte(0x9800, 200)

		expect:
		gpu.readTileIndex(0x9800, 0, 0, true) == 200
	}

	def "readTileIndex reads signed byte when useUnsigned is false"() {
		given: "tile map at 0x9800 has value 200 (signed: -56) at column 0, row 0"
		mmu.setByte(0x9800, 200)

		expect: "200 interpreted as signed byte is -56"
		gpu.readTileIndex(0x9800, 0, 0, false) == -56
	}

	def "readTileIndex computes correct address from tileMapBase, row, and column"() {
		given: "tile map at 0x9800 + row 1 (offset 32) + column 3 = 0x9823"
		mmu.setByte(0x9823, 42)

		expect:
		gpu.readTileIndex(0x9800, 32, 3, true) == 42
	}

	// === resolveTileDataAddress ===

	def "resolveTileDataAddress with unsigned mode computes address from 0x8000 base"() {
		expect: "tile index 2 at 0x8000: 0x8000 + (2 + 0) * 16 = 0x8020"
		gpu.resolveTileDataAddress(0x8000, true, 2) == 0x8020
	}

	def "resolveTileDataAddress with signed mode applies offset 128"() {
		expect: "tile index 0 at 0x8800 signed: 0x8800 + (0 + 128) * 16 = 0x9000"
		gpu.resolveTileDataAddress(0x8800, false, 0) == 0x9000
	}

	def "resolveTileDataAddress with signed mode and negative tile index"() {
		expect: "tile index -1 at 0x8800 signed: 0x8800 + (-1 + 128) * 16 = 0x8FF0"
		gpu.resolveTileDataAddress(0x8800, false, -1) == 0x8FF0
	}

	// === renderBackgroundPixel ===

	def "renderBackgroundPixel stores color index in backgroundColorIndices and writes shade"() {
		given: "enable BG rendering: LCDC = 0x91, BGP identity palette"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x91)
		mmu.setByte(MMU.PALETTE_BGP, 0xE4)

		when:
		gpu.renderBackgroundPixel(5, 2)

		then: "pixel 5 at line 0 has shade 2"
		gpu.frameBuffer.getPixels()[5][0] == 2
	}

	def "renderBackgroundPixel applies palette mapping for non-identity palette"() {
		given: "BGP palette maps color index 1 to shade 3"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x91)
		mmu.setByte(MMU.PALETTE_BGP, 0x0C)  // bits 3-2 = 11 → color 1 maps to shade 3

		when:
		gpu.renderBackgroundPixel(0, 1)

		then:
		gpu.frameBuffer.getPixels()[0][0] == 3
	}

	// === Helper methods ===

	private void advanceToLine(int targetLine) {
		for (int scanline = 0; scanline < targetLine; scanline++) {
			gpu.update(80)
			gpu.update(172)
			gpu.update(204)
		}
	}

	private void writeTileRow(int tileIndex, int row, int byte1, int byte2) {
		int address = 0x8000 + tileIndex * 16 + row * 2
		mmu.setByte(address, byte1)
		mmu.setByte(address + 1, byte2)
	}

	/**
	 * Renders a line to populate backgroundColorIndices, needed for isHiddenBehindBackground tests.
	 */
	private void renderLineForSetup() {
		gpu.update(80)
		gpu.update(172)
	}
}
