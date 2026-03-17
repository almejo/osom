package com.almejo.osom.memory

import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.input.Joypad
import spock.lang.Specification
import spock.lang.Unroll

class IORegisterStorageSpec extends Specification {

	MMU mmu
	Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	// === Generic I/O register write handler ===

	def "write to unhandled I/O register stores value for readback"() {
		when: "writing a value to an unhandled I/O address"
		mmu.setByte(MMU.PALETTE_BGP, 0xFC)

		then: "the value can be read back"
		mmu.getByte(MMU.PALETTE_BGP) == 0xFC
	}

	def "write to sound register stores value for readback"() {
		when: "writing to NR51 sound panning register"
		mmu.setByte(MMU.SOUND_NR51, 0xFF)

		then: "the value can be read back"
		mmu.getByte(MMU.SOUND_NR51) == 0xFF
	}

	def "multiple writes to same I/O register keep last value"() {
		when: "writing different values to the same register"
		mmu.setByte(MMU.PALETTE_BGP, 0xAA)
		mmu.setByte(MMU.PALETTE_BGP, 0xBB)

		then: "the last value written is returned"
		mmu.getByte(MMU.PALETTE_BGP) == 0xBB
	}

	def "write to I/O register masks to 8 bits"() {
		when: "writing a value larger than 8 bits"
		mmu.setByte(MMU.PALETTE_BGP, 0x1FC)

		then: "the value is masked to 8 bits"
		mmu.getByte(MMU.PALETTE_BGP) == 0xFC
	}

	// === Prohibited OAM area ===

	@Unroll
	def "write to prohibited OAM address #address is silently ignored"() {
		when: "writing to prohibited OAM area"
		mmu.setByte(address, 0x42)

		then: "read returns 0"
		mmu.getByte(address) == 0

		where:
		address << [0xFEA0, 0xFEB0, 0xFEC0, 0xFED0, 0xFEE0, 0xFEF0, 0xFEFF]
	}

	// === I/O register round-trip ===

	@Unroll
	def "palette register #registerName at #address supports round-trip"() {
		when: "writing to palette register"
		mmu.setByte(address, value)

		then: "the value can be read back"
		mmu.getByte(address) == value

		where:
		registerName | address          | value
		"BGP"        | MMU.PALETTE_BGP  | 0xFC
		"OBP0"       | MMU.PALETTE_OBP0 | 0xFF
		"OBP1"       | MMU.PALETTE_OBP1 | 0xE4
	}

	@Unroll
	def "window register #registerName at #address supports round-trip"() {
		when: "writing to window register"
		mmu.setByte(address, value)

		then: "the value can be read back"
		mmu.getByte(address) == value

		where:
		registerName | address      | value
		"WY"         | MMU.WINDOW_Y | 0x00
		"WX"         | MMU.WINDOW_X | 0x07
	}

	def "sound register NR51 supports round-trip"() {
		when: "writing to NR51"
		mmu.setByte(MMU.SOUND_NR51, 0xFF)

		then: "the value can be read back"
		mmu.getByte(MMU.SOUND_NR51) == 0xFF
	}

	@Unroll
	def "serial register #registerName at #address supports round-trip"() {
		when: "writing to serial register"
		mmu.setByte(address, value)

		then: "the value can be read back"
		mmu.getByte(address) == value

		where:
		registerName | address             | value
		"SB"         | MMU.SERIAL_DATA     | 0x42
		"SC"         | MMU.SERIAL_CONTROL  | 0x81
	}

	// === Non-interference with existing handlers ===

	def "write to joypad register delegates to joypad write handler"() {
		given: "a joypad mock is set"
		Joypad joypad = Mock(Joypad)
		mmu.setJoypad(joypad)

		when: "writing to joypad register"
		mmu.setByte(MMU.IO_REGISTER, 0x20)

		then: "joypad.write() is called"
		1 * joypad.write(0x20)
	}

	def "read from joypad register delegates to joypad read handler"() {
		given: "a joypad mock is set"
		Joypad joypad = Mock(Joypad)
		joypad.read() >> 0xCF
		mmu.setJoypad(joypad)

		when: "reading from joypad register"
		int result = mmu.getByte(MMU.IO_REGISTER)

		then: "joypad.read() is called and value returned"
		result == 0xCF
	}

	def "write to DIV resets to 0"() {
		given: "DIV has been incremented"
		mmu.incrementDividerRegister()
		mmu.incrementDividerRegister()

		when: "writing to DIV register"
		mmu.setByte(MMU.DIVIDER_REGISTER_ADDRESS, 0xFF)

		then: "DIV is reset to 0 (not stored as 0xFF)"
		mmu.getByte(MMU.DIVIDER_REGISTER_ADDRESS) == 0
	}

	def "write to DMA triggers DMA transfer and stores value for readback"() {
		given: "some data in source area"
		mmu.setByte(0xC000, 0xAA)
		mmu.setByte(0xC001, 0xBB)

		when: "writing to DMA register and completing DMA timing"
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)
		mmu.updateDma(MMU.DMA_DURATION_CYCLES)

		then: "DMA transfer copies data to OAM"
		mmu.getByte(0xFE00) == 0xAA
		mmu.getByte(0xFE01) == 0xBB

		and: "DMA register stores the written value for readback"
		mmu.getByte(MMU.DMA_ADDRESS) == 0xC0
	}

	def "write to IF uses existing handler"() {
		when: "writing to IF register"
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, 0x1F)

		then: "value is stored via existing handler"
		mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) == 0x1F
	}

	def "write to LCDC uses existing handler"() {
		when: "writing to LCDC register"
		mmu.setByte(MMU.LCD_CONTROLLER, 0x91)

		then: "value is stored via existing handler"
		mmu.getByte(MMU.LCD_CONTROLLER) == 0x91
	}

	def "write to STAT masks bits 0-2 as read-only"() {
		given: "STAT has mode bits set by GPU"
		mmu.setStatModeBits(0x03)

		when: "writing to STAT register"
		mmu.setByte(MMU.LCD_STATUS, 0x78)

		then: "bits 3-6 are written, bits 0-2 are preserved"
		(mmu.getByte(MMU.LCD_STATUS) & 0x07) == 0x03
		(mmu.getByte(MMU.LCD_STATUS) & 0x78) == 0x78
	}

	def "write to LY resets to 0"() {
		given: "LY has been set by GPU"
		mmu.setScanline(42)

		when: "writing to LY register"
		mmu.setByte(MMU.LCD_LINE_COUNTER, 0xFF)

		then: "LY is reset to 0 (not stored as 0xFF)"
		mmu.getByte(MMU.LCD_LINE_COUNTER) == 0
	}

	def "write to TIMA uses existing handler"() {
		when: "writing to TIMA register"
		mmu.setByte(MMU.TIMER_ADDRESS, 0x42)

		then: "value is stored via existing handler"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0x42
	}

	def "write to TMA uses existing handler"() {
		when: "writing to TMA register"
		mmu.setByte(MMU.TIMER_MODULATOR, 0x99)

		then: "value is stored via existing handler"
		mmu.getByte(MMU.TIMER_MODULATOR) == 0x99
	}

	def "write to TAC uses existing handler"() {
		when: "writing to TAC register"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)

		then: "value is stored via existing handler"
		mmu.getByte(MMU.TIMER_CONTROLLER) == 0x05
	}

	// === Edge cases ===

	def "I/O register reads default to 0 before any write"() {
		expect: "unwritten I/O registers return 0"
		mmu.getByte(MMU.PALETTE_BGP) == 0
		mmu.getByte(MMU.SOUND_NR51) == 0
		mmu.getByte(MMU.SERIAL_DATA) == 0
	}

	def "writing to different I/O registers does not cause cross-contamination"() {
		when: "writing to multiple I/O registers"
		mmu.setByte(MMU.PALETTE_BGP, 0xFC)
		mmu.setByte(MMU.PALETTE_OBP0, 0xE4)
		mmu.setByte(MMU.PALETTE_OBP1, 0x1B)

		then: "each register stores its own value independently"
		mmu.getByte(MMU.PALETTE_BGP) == 0xFC
		mmu.getByte(MMU.PALETTE_OBP0) == 0xE4
		mmu.getByte(MMU.PALETTE_OBP1) == 0x1B
	}
}
