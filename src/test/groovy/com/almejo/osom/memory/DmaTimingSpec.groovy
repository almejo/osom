package com.almejo.osom.memory

import com.almejo.osom.cpu.Z80Cpu
import spock.lang.Specification
import spock.lang.Unroll

class DmaTimingSpec extends Specification {

	MMU mmu
	Z80Cpu cpu

	def setup() {
		mmu = new MMU(false)
		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
	}

	// === Task 1 & 2: DMA sets active state and cycle countdown ===

	def "DMA transfer sets active state and cycle countdown"() {
		given: "source data in work RAM"
		mmu.setByte(0xC000, 0xAA)

		when: "writing to DMA register"
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		then: "DMA is active with full cycle countdown"
		mmu.isDmaActive()
	}

	def "DMA transfer copies bytes correctly"() {
		given: "source data across multiple addresses"
		for (int i = 0; i < 0xA0; i++) {
			mmu.setByte(0xC000 + i, i & 0xFF)
		}

		when: "triggering DMA and completing it"
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)
		mmu.updateDma(MMU.DMA_DURATION_CYCLES)

		then: "all 160 bytes were copied to OAM"
		for (int i = 0; i < 0xA0; i++) {
			assert mmu.getByte(0xFE00 + i) == (i & 0xFF)
		}
	}

	// === Task 3: updateDma() cycle countdown ===

	def "updateDma deactivates DMA after correct cycle count"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "updating with exactly DMA_DURATION_CYCLES"
		mmu.updateDma(MMU.DMA_DURATION_CYCLES)

		then: "DMA is no longer active"
		!mmu.isDmaActive()
	}

	def "updateDma deactivates DMA when cycles exceed duration"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "updating with more cycles than needed"
		mmu.updateDma(MMU.DMA_DURATION_CYCLES + 100)

		then: "DMA is no longer active"
		!mmu.isDmaActive()
	}

	def "updateDma keeps DMA active when not enough cycles"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "updating with fewer cycles than needed"
		mmu.updateDma(100)

		then: "DMA remains active"
		mmu.isDmaActive()
	}

	def "updateDma deactivates after multiple partial updates"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "updating in small increments totaling DMA duration"
		mmu.updateDma(200)
		mmu.updateDma(200)
		mmu.updateDma(200)
		mmu.updateDma(40)

		then: "DMA is no longer active"
		!mmu.isDmaActive()
	}

	def "updateDma is a no-op when DMA is inactive"() {
		expect: "DMA is not active initially"
		!mmu.isDmaActive()

		when: "calling updateDma"
		mmu.updateDma(100)

		then: "no error and DMA remains inactive"
		!mmu.isDmaActive()
	}

	// === Task 4: Memory access restrictions during DMA ===

	def "DMA active blocks reads outside HRAM returning 0xFF"() {
		given: "data in various memory regions"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(0x8000, 0x33)

		and: "DMA is active"
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		expect: "reads outside HRAM return 0xFF"
		mmu.getByte(0xC000) == 0xFF
		mmu.getByte(0x8000) == 0xFF
	}

	def "DMA active blocks writes outside HRAM"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "writing to work RAM during DMA"
		mmu.setByte(0xC100, 0xBB)

		and: "DMA completes"
		mmu.updateDma(MMU.DMA_DURATION_CYCLES)

		then: "the write during DMA was ignored"
		mmu.getByte(0xC100) == 0x00
	}

	def "HRAM reads succeed during DMA"() {
		given: "data in HRAM"
		mmu.setByte(0xFF80, 0xAA)
		mmu.setByte(0xFFFE, 0xBB)

		and: "DMA is active"
		mmu.setByte(0xC000, 0x01)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		expect: "HRAM reads return correct values"
		mmu.getByte(0xFF80) == 0xAA
		mmu.getByte(0xFFFE) == 0xBB
	}

	def "HRAM writes succeed during DMA"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x01)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "writing to HRAM during DMA"
		mmu.setByte(0xFF90, 0xCC)

		then: "HRAM write succeeds"
		mmu.getByte(0xFF90) == 0xCC
	}

	def "IE register (0xFFFF) accessible during DMA"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x01)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "writing to IE register during DMA"
		mmu.setByte(0xFFFF, 0x1F)

		then: "IE register write succeeds"
		mmu.getByte(0xFFFF) == 0x1F
	}

	def "DMA register (0xFF46) accessible during DMA for re-triggering"() {
		given: "DMA is active from first trigger"
		mmu.setByte(0xC000, 0x11)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		expect: "DMA register is readable"
		mmu.getByte(MMU.DMA_ADDRESS) == 0xC0
	}

	// === Task 2 (AC 7): Re-triggering DMA ===

	def "re-triggering DMA resets cycle countdown"() {
		given: "source data for both DMA triggers"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(0xD000, 0x77)

		and: "DMA is active"
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		and: "some cycles have elapsed"
		mmu.updateDma(300)

		when: "re-triggering DMA with new source"
		mmu.setByte(MMU.DMA_ADDRESS, 0xD0)

		then: "DMA is still active"
		mmu.isDmaActive()

		when: "updating with less than full duration (but enough to complete original)"
		mmu.updateDma(400)

		then: "DMA is still active because countdown was reset"
		mmu.isDmaActive()

		when: "updating with remaining cycles"
		mmu.updateDma(240)

		then: "DMA completes"
		!mmu.isDmaActive()
	}

	def "re-triggering DMA copies new source data"() {
		given: "both source areas have data"
		mmu.setByte(0xC000, 0xAA)
		mmu.setByte(0xD000, 0xBB)

		and: "first DMA trigger"
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "re-triggering DMA with new source"
		mmu.setByte(MMU.DMA_ADDRESS, 0xD0)

		and: "DMA completes"
		mmu.updateDma(MMU.DMA_DURATION_CYCLES)

		then: "OAM contains data from second source"
		mmu.getByte(0xFE00) == 0xBB
	}

	// === Edge cases ===

	@Unroll
	def "read from #description (address 0x#addressHex) returns 0xFF during DMA"() {
		given: "DMA is active"
		mmu.setByte(0xC000, 0x01)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		expect: "read returns 0xFF"
		mmu.getByte(address) == 0xFF

		where:
		description  | address | addressHex
		"work RAM"   | 0xC000  | "C000"
		"VRAM"       | 0x8000  | "8000"
		"OAM"        | 0xFE00  | "FE00"
		"I/O"        | 0xFF01  | "FF01"
	}

	@Unroll
	def "read from HRAM address 0x#addressHex succeeds during DMA"() {
		given: "data in HRAM"
		mmu.setByte(address, 0x42)

		and: "DMA is active"
		mmu.setByte(0xC000, 0x01)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		expect: "HRAM read succeeds"
		mmu.getByte(address) == 0x42

		where:
		address | addressHex
		0xFF80  | "FF80"
		0xFFA0  | "FFA0"
		0xFFD0  | "FFD0"
		0xFFFE  | "FFFE"
	}

	def "full memory access restored after DMA completes"() {
		given: "data in work RAM"
		mmu.setByte(0xC000, 0x42)

		and: "DMA is triggered and completed"
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)
		mmu.updateDma(MMU.DMA_DURATION_CYCLES)

		expect: "reads outside HRAM work normally again"
		mmu.getByte(0xC000) == 0x42
	}
}
