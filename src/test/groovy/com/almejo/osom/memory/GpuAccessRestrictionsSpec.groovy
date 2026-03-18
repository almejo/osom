package com.almejo.osom.memory

import com.almejo.osom.gpu.GPU
import spock.lang.Specification

class GpuAccessRestrictionsSpec extends Specification {
	MMU mmu

	def setup() {
		mmu = new MMU(false)
	}

	private void enableLcd() {
		mmu.setByte(MMU.LCD_CONTROLLER, 0x80)
	}

	private void disableLcd() {
		mmu.setByte(MMU.LCD_CONTROLLER, 0x00)
	}

	// --- Task 6: OAM access restrictions ---

	def "Mode 2 (OAM Search): OAM read returns 0xFF"() {
		given: "LCD enabled, Mode 2, OAM has data"
		enableLcd()
		mmu.setStatModeBits(GPU.SPRITES)
		mmu.setByte(MMU.LCD_CONTROLLER, 0x80) // re-enable after setStatModeBits
		// Pre-load OAM while in accessible mode
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0xFE00, 0x42)
		mmu.setStatModeBits(GPU.SPRITES)

		when: "CPU reads OAM"
		int result = mmu.getByte(0xFE00)

		then: "returns 0xFF"
		result == 0xFF
	}

	def "Mode 2 (OAM Search): OAM write is silently ignored"() {
		given: "LCD enabled, data pre-loaded in OAM, then switch to Mode 2"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0xFE00, 0x42)
		mmu.setStatModeBits(GPU.SPRITES)

		when: "CPU writes to OAM"
		mmu.setByte(0xFE00, 0x99)

		then: "original value unchanged (write was ignored)"
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.getByte(0xFE00) == 0x42
	}

	def "Mode 0 (H-Blank): OAM read succeeds"() {
		given: "LCD enabled, Mode 0, OAM has data"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0xFE00, 0x42)

		when: "CPU reads OAM"
		int result = mmu.getByte(0xFE00)

		then: "returns actual value"
		result == 0x42
	}

	def "Mode 1 (V-Blank): OAM read succeeds"() {
		given: "LCD enabled, Mode 1, OAM has data"
		enableLcd()
		mmu.setStatModeBits(GPU.V_BLANK)
		mmu.setByte(0xFE00, 0x55)

		when: "CPU reads OAM"
		int result = mmu.getByte(0xFE00)

		then: "returns actual value"
		result == 0x55
	}

	def "LCD off: OAM read succeeds regardless of mode bits"() {
		given: "LCD disabled, mode bits set to Mode 2"
		disableLcd()
		mmu.setStatModeBits(GPU.SPRITES)
		// Write directly — LCD off means no restrictions
		mmu.setByte(0xFE00, 0x77)

		when: "CPU reads OAM"
		int result = mmu.getByte(0xFE00)

		then: "returns actual value (no restriction when LCD off)"
		result == 0x77
	}

	// --- Task 7: VRAM access restrictions ---

	def "Mode 3 (Pixel Transfer): VRAM read returns 0xFF"() {
		given: "LCD enabled, VRAM has data, then switch to Mode 3"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x8000, 0xAB)
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU reads VRAM"
		int result = mmu.getByte(0x8000)

		then: "returns 0xFF"
		result == 0xFF
	}

	def "Mode 3 (Pixel Transfer): VRAM write is silently ignored"() {
		given: "LCD enabled, VRAM has data, then switch to Mode 3"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x8000, 0xAB)
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU writes to VRAM"
		mmu.setByte(0x8000, 0xCD)

		then: "original value unchanged"
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.getByte(0x8000) == 0xAB
	}

	def "Mode 3 (Pixel Transfer): OAM read also returns 0xFF"() {
		given: "LCD enabled, OAM has data, then switch to Mode 3"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0xFE00, 0x42)
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU reads OAM"
		int result = mmu.getByte(0xFE00)

		then: "returns 0xFF (both OAM and VRAM blocked in Mode 3)"
		result == 0xFF
	}

	def "Mode 2 (OAM Search): VRAM read succeeds (only OAM blocked)"() {
		given: "LCD enabled, VRAM has data, Mode 2"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x8000, 0xBE)
		mmu.setStatModeBits(GPU.SPRITES)

		when: "CPU reads VRAM"
		int result = mmu.getByte(0x8000)

		then: "returns actual value (VRAM accessible in Mode 2)"
		result == 0xBE
	}

	def "Mode 0 (H-Blank): VRAM read succeeds"() {
		given: "LCD enabled, Mode 0, VRAM has data"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x8000, 0xDE)

		when: "CPU reads VRAM"
		int result = mmu.getByte(0x8000)

		then: "returns actual value"
		result == 0xDE
	}

	def "LCD off: VRAM read succeeds regardless of mode bits"() {
		given: "LCD disabled, mode bits set to Mode 3"
		disableLcd()
		mmu.setStatModeBits(GPU.GRAPHICS)
		mmu.setByte(0x8000, 0xEF)

		when: "CPU reads VRAM"
		int result = mmu.getByte(0x8000)

		then: "returns actual value (no restriction when LCD off)"
		result == 0xEF
	}

	// --- Task 8: PPU self-access bypass ---

	def "PPU access flag true, Mode 3: VRAM read succeeds"() {
		given: "LCD enabled, Mode 3, VRAM has data, PPU access in progress"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x8000, 0xAB)
		mmu.setStatModeBits(GPU.GRAPHICS)
		mmu.setPpuAccessInProgress(true)

		when: "read VRAM (simulating GPU internal read)"
		int result = mmu.getByte(0x8000)

		then: "returns actual value (PPU bypasses restriction)"
		result == 0xAB

		cleanup:
		mmu.setPpuAccessInProgress(false)
	}

	def "PPU access flag true, Mode 3: OAM read succeeds"() {
		given: "LCD enabled, Mode 3, OAM has data, PPU access in progress"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0xFE00, 0x42)
		mmu.setStatModeBits(GPU.GRAPHICS)
		mmu.setPpuAccessInProgress(true)

		when: "read OAM (simulating GPU internal read)"
		int result = mmu.getByte(0xFE00)

		then: "returns actual value (PPU bypasses restriction)"
		result == 0x42

		cleanup:
		mmu.setPpuAccessInProgress(false)
	}

	def "PPU access flag false, Mode 3: VRAM read returns 0xFF (confirmation)"() {
		given: "LCD enabled, Mode 3, VRAM has data, PPU access NOT in progress"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x8000, 0xAB)
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU reads VRAM (no PPU flag)"
		int result = mmu.getByte(0x8000)

		then: "returns 0xFF (restriction active for CPU)"
		result == 0xFF
	}

	// --- Task 9: DMA + GPU guard coexistence ---

	def "DMA active + Mode 3: CPU read of VRAM returns 0xFF (DMA fires first)"() {
		given: "LCD enabled, Mode 3, DMA active"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x8000, 0xAB)
		mmu.setStatModeBits(GPU.GRAPHICS)
		// Trigger DMA to make it active
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "CPU reads VRAM (both DMA and GPU restrictions apply)"
		int result = mmu.getByte(0x8000)

		then: "returns 0xFF (DMA restriction fires first — blocks all non-HRAM)"
		result == 0xFF
	}

	def "DMA active + Mode 2: CPU read of HRAM succeeds (DMA allows HRAM)"() {
		given: "LCD enabled, Mode 2, DMA active, HRAM has data"
		enableLcd()
		mmu.setByte(0xFF80, 0x33)
		mmu.setStatModeBits(GPU.SPRITES)
		// Trigger DMA
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "CPU reads HRAM"
		int result = mmu.getByte(0xFF80)

		then: "returns actual value (DMA allows HRAM access)"
		result == 0x33
	}

	// --- Additional edge cases ---

	def "Mode 2: OAM write blocked at last OAM address 0xFE9F"() {
		given: "LCD enabled, data pre-loaded at end of OAM range"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0xFE9F, 0x11)
		mmu.setStatModeBits(GPU.SPRITES)

		when: "CPU writes to last OAM address"
		mmu.setByte(0xFE9F, 0x22)

		then: "write ignored, original value preserved"
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.getByte(0xFE9F) == 0x11
	}

	def "Mode 3: VRAM write blocked at last VRAM address 0x9FFF"() {
		given: "LCD enabled, data pre-loaded at end of VRAM range"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0x9FFF, 0x11)
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU writes to last VRAM address"
		mmu.setByte(0x9FFF, 0x22)

		then: "write ignored, original value preserved"
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.getByte(0x9FFF) == 0x11
	}

	def "Mode 3: OAM write blocked"() {
		given: "LCD enabled, OAM data pre-loaded, Mode 3"
		enableLcd()
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.setByte(0xFE10, 0xAA)
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU writes to OAM"
		mmu.setByte(0xFE10, 0xBB)

		then: "write ignored"
		mmu.setStatModeBits(GPU.H_BLANK)
		mmu.getByte(0xFE10) == 0xAA
	}

	def "LCD off: OAM write succeeds even with mode bits set to Mode 3"() {
		given: "LCD disabled, mode bits set to Mode 3"
		disableLcd()
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU writes to OAM"
		mmu.setByte(0xFE00, 0xCC)

		then: "write succeeds"
		mmu.getByte(0xFE00) == 0xCC
	}

	def "LCD off: VRAM write succeeds even with mode bits set to Mode 3"() {
		given: "LCD disabled, mode bits set to Mode 3"
		disableLcd()
		mmu.setStatModeBits(GPU.GRAPHICS)

		when: "CPU writes to VRAM"
		mmu.setByte(0x8000, 0xDD)

		then: "write succeeds"
		mmu.getByte(0x8000) == 0xDD
	}
}
