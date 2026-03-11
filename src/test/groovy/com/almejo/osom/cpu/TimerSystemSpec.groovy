package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class TimerSystemSpec extends Specification {

	MMU mmu
	Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	// === Task 1: TIMA and TMA write handlers ===

	def "writing to TIMA (0xFF05) via setByte stores the value"() {
		when: "writing a value to TIMA"
		mmu.setByte(MMU.TIMER_ADDRESS, 0x42)

		then: "reading TIMA returns the written value"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0x42
	}

	def "writing to TMA (0xFF06) via setByte stores the value"() {
		when: "writing a value to TMA"
		mmu.setByte(MMU.TIMER_MODULATOR, 0xE0)

		then: "reading TMA returns the written value"
		mmu.getByte(MMU.TIMER_MODULATOR) == 0xE0
	}

	def "CPU resetMemory initializes TIMA to 0x00"() {
		given: "TIMA has a non-zero value"
		mmu.@ram[MMU.TIMER_ADDRESS] = 0xFF

		when: "CPU reset is called (no bios)"
		cpu.reset(false)

		then: "TIMA is reset to 0x00"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0x00
	}

	def "CPU resetMemory initializes TMA to 0x00"() {
		given: "TMA has a non-zero value"
		mmu.@ram[MMU.TIMER_MODULATOR] = 0xFF

		when: "CPU reset is called (no bios)"
		cpu.reset(false)

		then: "TMA is reset to 0x00"
		mmu.getByte(MMU.TIMER_MODULATOR) == 0x00
	}

	// === Task 2: TAC enable-only changes must store value ===

	def "writing TAC with enable bit only (0x04) stores value and enables timer"() {
		when: "writing TAC=0x04 (enable timer, 4096Hz) via setByte"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x04)

		then: "TAC register contains the written value"
		mmu.getByte(MMU.TIMER_CONTROLLER) == 0x04

		and: "timer is enabled"
		cpu.isClockEnabled()
	}

	def "writing TAC=0x00 after 0x04 disables the timer"() {
		given: "timer is enabled with TAC=0x04"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x04)

		when: "writing TAC=0x00 (disable timer)"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x00)

		then: "TAC register is updated"
		mmu.getByte(MMU.TIMER_CONTROLLER) == 0x00

		and: "timer is disabled"
		!cpu.isClockEnabled()
	}

	// === Task 3: convertToTimerCycles masking ===

	def "convertToTimerCycles returns correct cycles for each TAC frequency"() {
		expect: "each frequency value maps to the correct T-cycles per tick"
		cpu.convertToTimerCycles(0) == 1024  // 4194304 / 4096
		cpu.convertToTimerCycles(1) == 16    // 4194304 / 262144
		cpu.convertToTimerCycles(2) == 64    // 4194304 / 65536
		cpu.convertToTimerCycles(3) == 256   // 4194304 / 16384
	}

	def "convertToTimerCycles masks input to lower 2 bits"() {
		expect: "values with enable bit set (bit 2) are masked correctly"
		cpu.convertToTimerCycles(4) == 1024  // 0b100 -> masked to 0b00 -> 4096Hz
		cpu.convertToTimerCycles(5) == 16    // 0b101 -> masked to 0b01 -> 262144Hz
		cpu.convertToTimerCycles(6) == 64    // 0b110 -> masked to 0b10 -> 65536Hz
		cpu.convertToTimerCycles(7) == 256   // 0b111 -> masked to 0b11 -> 16384Hz
	}

	// === Task 4: timerCounter reset after TIMA increment ===

	def "TIMA increments exactly twice after 2048 cycles at 4096Hz"() {
		given: "timer enabled at 4096Hz (1024 T-cycles per tick), TIMA starts at 0"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x04)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x00)

		when: "accumulating exactly 2048 T-cycles (2 ticks worth) in 4-cycle steps"
		for (int i = 0; i < 512; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA has incremented exactly 2 times"
		mmu.getByte(MMU.TIMER_ADDRESS) == 2
	}

	def "TIMA does not run away after first tick"() {
		given: "timer enabled at 262144Hz (16 T-cycles per tick), TIMA starts at 0"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x00)

		when: "accumulating exactly 64 T-cycles (4 ticks worth) in 4-cycle steps"
		for (int i = 0; i < 16; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA has incremented exactly 4 times (not a runaway value)"
		mmu.getByte(MMU.TIMER_ADDRESS) == 4
	}

	// === Task 5: Divider register threshold ===

	def "divider register increments after exactly 256 T-cycles"() {
		when: "accumulating exactly 256 T-cycles in 4-cycle steps"
		for (int i = 0; i < 64; i++) {
			cpu.updateTimers(4)
		}

		then: "divider has incremented exactly once"
		mmu.getByte(0xFF04) == 1
	}

	def "divider register does NOT increment at 255 T-cycles"() {
		when: "accumulating exactly 255 T-cycles in a single call"
		cpu.updateTimers(255)

		then: "divider has not incremented (threshold is 256, not 255)"
		mmu.getByte(0xFF04) == 0
	}

	def "divider register preserves cycle overshoot"() {
		when: "accumulating 520 T-cycles (2 * 256 + 8 overshoot) in 8-cycle steps"
		for (int i = 0; i < 65; i++) {
			cpu.updateTimers(8)
		}

		then: "divider has incremented exactly twice (overshoot from first increment carries over)"
		mmu.getByte(0xFF04) == 2
	}

	def "writing any value to 0xFF04 resets divider to 0"() {
		given: "divider has incremented to a non-zero value"
		for (int i = 0; i < 128; i++) {
			cpu.updateTimers(4)
		}
		assert mmu.getByte(0xFF04) == 2

		when: "writing any value to the divider register"
		mmu.setByte(0xFF04, 0x42)

		then: "divider is reset to 0"
		mmu.getByte(0xFF04) == 0
	}

	// === Task 6: Comprehensive integration tests ===

	def "TIMA increments at 262144Hz (16 T-cycles per tick)"() {
		given: "timer enabled at 262144Hz, TIMA starts at 0"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x00)

		when: "accumulating exactly 48 T-cycles (3 ticks)"
		for (int i = 0; i < 12; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA == 3"
		mmu.getByte(MMU.TIMER_ADDRESS) == 3
	}

	def "TIMA increments at 65536Hz (64 T-cycles per tick)"() {
		given: "timer enabled at 65536Hz, TIMA starts at 0"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x06)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x00)

		when: "accumulating exactly 192 T-cycles (3 ticks)"
		for (int i = 0; i < 48; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA == 3"
		mmu.getByte(MMU.TIMER_ADDRESS) == 3
	}

	def "TIMA increments at 16384Hz (256 T-cycles per tick)"() {
		given: "timer enabled at 16384Hz, TIMA starts at 0"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x07)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x00)

		when: "accumulating exactly 768 T-cycles (3 ticks)"
		for (int i = 0; i < 192; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA == 3"
		mmu.getByte(MMU.TIMER_ADDRESS) == 3
	}

	def "TIMA overflow reloads from TMA and fires timer interrupt"() {
		given: "timer enabled at 262144Hz, TIMA at 0xFF (about to overflow), TMA at 0xE0"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)
		mmu.setByte(MMU.TIMER_ADDRESS, 0xFF)
		mmu.setByte(MMU.TIMER_MODULATOR, 0xE0)

		when: "one TIMA tick occurs (16 T-cycles)"
		for (int i = 0; i < 4; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA has been reloaded from TMA"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0xE0

		and: "timer interrupt bit (bit 2) is set in IF register"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x04) == 0x04
	}

	def "TIMA overflow with TMA=0x00 reloads to 0 and counts 256 ticks per overflow"() {
		given: "timer enabled at 262144Hz, TIMA at 0xFE, TMA at 0x00"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)
		mmu.setByte(MMU.TIMER_ADDRESS, 0xFE)
		mmu.setByte(MMU.TIMER_MODULATOR, 0x00)

		when: "two TIMA ticks occur (32 T-cycles)"
		for (int i = 0; i < 8; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA overflowed and reloaded from TMA (0x00)"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0x00

		and: "timer interrupt was fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x04) == 0x04
	}

	def "TIMA overflow with TMA=0xFF fires interrupt every tick"() {
		given: "timer enabled at 262144Hz, TIMA at 0xFF, TMA at 0xFF"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)
		mmu.setByte(MMU.TIMER_ADDRESS, 0xFF)
		mmu.setByte(MMU.TIMER_MODULATOR, 0xFF)

		when: "one TIMA tick occurs"
		for (int i = 0; i < 4; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA reloaded to 0xFF (from TMA)"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0xFF

		and: "timer interrupt was fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x04) == 0x04
	}

	def "timer disabled (TAC bit 2 = 0) does not increment TIMA"() {
		given: "timer disabled (TAC=0x00), TIMA starts at 0x00"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x00)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x00)

		when: "accumulating many T-cycles"
		for (int i = 0; i < 1000; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA has not changed"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0x00
	}

	def "timer stays disabled at startup (TAC defaults to 0x00)"() {
		expect: "TAC defaults to 0x00 (timer disabled)"
		mmu.getByte(MMU.TIMER_CONTROLLER) == 0x00

		and: "timer is disabled"
		!cpu.isClockEnabled()
	}

	// === Code review fixes: while-loop for multiple ticks per call ===

	def "DIV increments multiple times when cycles exceed 512"() {
		when: "accumulating 520 T-cycles in a single call (2 full DIV periods + 8 overshoot)"
		cpu.updateTimers(520)

		then: "divider has incremented exactly twice"
		mmu.getByte(0xFF04) == 2
	}

	def "TIMA increments multiple times at 262144Hz when cycles exceed one tick"() {
		given: "timer enabled at 262144Hz (16 T-cycles per tick), TIMA starts at 0"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x00)

		when: "accumulating 48 T-cycles in a single call (3 ticks worth)"
		cpu.updateTimers(48)

		then: "TIMA has incremented exactly 3 times"
		mmu.getByte(MMU.TIMER_ADDRESS) == 3
	}

	def "TIMA handles overflow during multi-tick single call"() {
		given: "timer enabled at 262144Hz, TIMA at 0xFE, TMA at 0x10"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x05)
		mmu.setByte(MMU.TIMER_ADDRESS, 0xFE)
		mmu.setByte(MMU.TIMER_MODULATOR, 0x10)

		when: "accumulating 48 T-cycles (3 ticks: 0xFE -> 0xFF -> overflow+reload to 0x10 -> 0x11)"
		cpu.updateTimers(48)

		then: "TIMA overflowed, reloaded from TMA, and continued counting"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0x11

		and: "timer interrupt was fired"
		(mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS) & 0x04) == 0x04
	}

	def "timer enable/disable toggle with same frequency preserves TIMA"() {
		given: "timer enabled at 4096Hz, TIMA incremented to 5"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x04)
		mmu.setByte(MMU.TIMER_ADDRESS, 0x05)

		when: "disabling the timer"
		mmu.setByte(MMU.TIMER_CONTROLLER, 0x00)

		and: "accumulating many cycles while disabled"
		for (int i = 0; i < 500; i++) {
			cpu.updateTimers(4)
		}

		then: "TIMA value is preserved at 5"
		mmu.getByte(MMU.TIMER_ADDRESS) == 0x05
	}
}
