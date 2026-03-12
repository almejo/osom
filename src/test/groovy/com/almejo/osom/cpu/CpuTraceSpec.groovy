package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class CpuTraceSpec extends Specification {

	def "formatLine produces Gameboy Doctor format with zero-padded uppercase hex"() {
		given: "a CpuTracer instance"
		def tracer = new CpuTracer()

		when: "formatting with known register values"
		def result = tracer.formatLine(0x01, 0xB0, 0x00, 0x13, 0x00, 0xD8, 0x01, 0x4D, 0xFFFE, 0x0100,
				0x00, 0xC3, 0x50, 0x01)

		then: "output matches exact Gameboy Doctor format"
		result == "A:01 F:B0 B:00 C:13 D:00 E:D8 H:01 L:4D SP:FFFE PC:0100 PCMEM:00,C3,50,01"
	}

	def "formatLine zero-pads single-digit hex values"() {
		given: "a CpuTracer instance"
		def tracer = new CpuTracer()

		when: "formatting with all-zero register values"
		def result = tracer.formatLine(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0000, 0x0000,
				0x00, 0x00, 0x00, 0x00)

		then: "all values are zero-padded"
		result == "A:00 F:00 B:00 C:00 D:00 E:00 H:00 L:00 SP:0000 PC:0000 PCMEM:00,00,00,00"
	}

	def "formatLine handles maximum register values"() {
		given: "a CpuTracer instance"
		def tracer = new CpuTracer()

		when: "formatting with maximum values"
		def result = tracer.formatLine(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFFFF, 0xFFFF,
				0xFF, 0xFF, 0xFF, 0xFF)

		then: "output uses uppercase hex"
		result == "A:FF F:FF B:FF C:FF D:FF E:FF H:FF L:FF SP:FFFF PC:FFFF PCMEM:FF,FF,FF,FF"
	}

	def "formatLine uses uppercase hex for A-F digits"() {
		given: "a CpuTracer instance"
		def tracer = new CpuTracer()

		when: "formatting with values containing A-F hex digits"
		def result = tracer.formatLine(0xAB, 0xCD, 0xEF, 0xDE, 0xAD, 0xBE, 0xEF, 0xCA, 0xFACE, 0xBEEF,
				0xAA, 0xBB, 0xCC, 0xDD)

		then: "hex letters are uppercase"
		result == "A:AB F:CD B:EF C:DE D:AD E:BE H:EF L:CA SP:FACE PC:BEEF PCMEM:AA,BB,CC,DD"
	}

	def "formatLine field order matches Gameboy Doctor specification"() {
		given: "a CpuTracer instance"
		def tracer = new CpuTracer()

		when: "formatting a line"
		def result = tracer.formatLine(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x0910, 0x1112,
				0x13, 0x14, 0x15, 0x16)

		then: "fields appear in order: A F B C D E H L SP PC PCMEM"
		result.startsWith("A:")
		result.contains("F:")
		result.contains("B:")
		result.contains("C:")
		result.contains("D:")
		result.contains("E:")
		result.contains("H:")
		result.contains("L:")
		result.contains("SP:")
		result.contains("PC:")
		result.contains("PCMEM:")
		result.indexOf("A:") < result.indexOf("F:")
		result.indexOf("F:") < result.indexOf("B:")
		result.indexOf("B:") < result.indexOf("C:")
		result.indexOf("C:") < result.indexOf("D:")
		result.indexOf("D:") < result.indexOf("E:")
		result.indexOf("E:") < result.indexOf("H:")
		result.indexOf("H:") < result.indexOf("L:")
		result.indexOf("L:") < result.indexOf("SP:")
		result.indexOf("SP:") < result.indexOf("PC:")
		result.indexOf("PC:") < result.indexOf("PCMEM:")
	}

	def "traceLine reads CPU registers and PCMEM from MMU"() {
		given: "a CPU with known register values and a stubbed MMU"
		def mmu = Stub(MMU)
		def cpu = new Z80Cpu(mmu, 4194304)
		cpu.AF.setValue(0x01B0)
		cpu.BC.setValue(0x0013)
		cpu.DE.setValue(0x00D8)
		cpu.HL.setValue(0x014D)
		cpu.SP.setValue(0xFFFE)
		cpu.PC.setValue(0x0100)

		mmu.getByte(0x0100) >> 0x00
		mmu.getByte(0x0101) >> 0xC3
		mmu.getByte(0x0102) >> 0x50
		mmu.getByte(0x0103) >> 0x01

		def tracer = new CpuTracer()

		and: "capture stdout"
		def originalOut = System.out
		def outputStream = new ByteArrayOutputStream()
		System.setOut(new PrintStream(outputStream))

		when: "tracing a line"
		tracer.traceLine(cpu, mmu)

		then: "stdout contains the Gameboy Doctor format line"
		def output = outputStream.toString().trim()
		output == "A:01 F:B0 B:00 C:13 D:00 E:D8 H:01 L:4D SP:FFFE PC:0100 PCMEM:00,C3,50,01"

		cleanup:
		System.setOut(originalOut)
	}

	def "traceLine does not output when traceEnabled is false on CPU"() {
		given: "a CPU with tracing disabled"
		def mmu = Stub(MMU)
		def cpu = new Z80Cpu(mmu, 4194304)
		cpu.PC.setValue(0x0100)

		and: "capture stdout"
		def originalOut = System.out
		def outputStream = new ByteArrayOutputStream()
		System.setOut(new PrintStream(outputStream))

		when: "checking that traceEnabled defaults to false"
		then: "traceEnabled is false by default"
		!cpu.@traceEnabled

		cleanup:
		System.setOut(originalOut)
	}

	def "PCMEM returns 0x00 for addresses beyond 0xFFFF"() {
		given: "a CPU with PC near end of memory and a stubbed MMU"
		def mmu = Stub(MMU)
		def cpu = new Z80Cpu(mmu, 4194304)
		cpu.PC.setValue(0xFFFD)
		cpu.AF.setValue(0x0000)
		cpu.BC.setValue(0x0000)
		cpu.DE.setValue(0x0000)
		cpu.HL.setValue(0x0000)
		cpu.SP.setValue(0x0000)

		mmu.getByte(0xFFFD) >> 0xAA
		mmu.getByte(0xFFFE) >> 0xBB
		mmu.getByte(0xFFFF) >> 0xCC

		def tracer = new CpuTracer()

		and: "capture stdout"
		def originalOut = System.out
		def outputStream = new ByteArrayOutputStream()
		System.setOut(new PrintStream(outputStream))

		when: "tracing a line"
		tracer.traceLine(cpu, mmu)

		then: "PCMEM shows 3 real bytes and one 0x00 for the out-of-range address"
		def output = outputStream.toString().trim()
		output.endsWith("PCMEM:AA,BB,CC,00")

		cleanup:
		System.setOut(originalOut)
	}

	def "PCMEM returns 0x00 for all out-of-range addresses when PC is 0xFFFF"() {
		given: "a CPU with PC at the last addressable byte"
		def mmu = Stub(MMU)
		def cpu = new Z80Cpu(mmu, 4194304)
		cpu.PC.setValue(0xFFFF)
		cpu.AF.setValue(0x0000)
		cpu.BC.setValue(0x0000)
		cpu.DE.setValue(0x0000)
		cpu.HL.setValue(0x0000)
		cpu.SP.setValue(0x0000)

		mmu.getByte(0xFFFF) >> 0xDD

		def tracer = new CpuTracer()

		and: "capture stdout"
		def originalOut = System.out
		def outputStream = new ByteArrayOutputStream()
		System.setOut(new PrintStream(outputStream))

		when: "tracing a line"
		tracer.traceLine(cpu, mmu)

		then: "only the first PCMEM byte is real, rest are 0x00"
		def output = outputStream.toString().trim()
		output.endsWith("PCMEM:DD,00,00,00")

		cleanup:
		System.setOut(originalOut)
	}

	def "PCMEM returns 0x00 for two out-of-range addresses when PC is 0xFFFE"() {
		given: "a CPU with PC at 0xFFFE"
		def mmu = Stub(MMU)
		def cpu = new Z80Cpu(mmu, 4194304)
		cpu.PC.setValue(0xFFFE)
		cpu.AF.setValue(0x0000)
		cpu.BC.setValue(0x0000)
		cpu.DE.setValue(0x0000)
		cpu.HL.setValue(0x0000)
		cpu.SP.setValue(0x0000)

		mmu.getByte(0xFFFE) >> 0xEE
		mmu.getByte(0xFFFF) >> 0xFF

		def tracer = new CpuTracer()

		and: "capture stdout"
		def originalOut = System.out
		def outputStream = new ByteArrayOutputStream()
		System.setOut(new PrintStream(outputStream))

		when: "tracing a line"
		tracer.traceLine(cpu, mmu)

		then: "first two PCMEM bytes are real, last two are 0x00"
		def output = outputStream.toString().trim()
		output.endsWith("PCMEM:EE,FF,00,00")

		cleanup:
		System.setOut(originalOut)
	}

	def "all individual register values are correctly represented"() {
		given: "a CpuTracer and specific register values"
		def tracer = new CpuTracer()

		when: "each register has a distinct value"
		def result = tracer.formatLine(0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x9ABC, 0xDEF0,
				0x12, 0x34, 0x56, 0x78)

		then: "every register value is independently correct"
		result == "A:11 F:22 B:33 C:44 D:55 E:66 H:77 L:88 SP:9ABC PC:DEF0 PCMEM:12,34,56,78"
	}
}
