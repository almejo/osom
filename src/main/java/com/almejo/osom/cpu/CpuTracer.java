package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

public class CpuTracer {

	/**
	 * Formats CPU state into Gameboy Doctor format line.
	 * All 8-bit registers are zero-padded to 2 uppercase hex digits.
	 * SP and PC are zero-padded to 4 uppercase hex digits.
	 * PCMEM contains 4 bytes from memory at PC..PC+3, comma-separated.
	 */
	public String formatLine(int a, int f, int b, int c, int d, int e, int h, int l,
							 int sp, int pc, int pcmem0, int pcmem1, int pcmem2, int pcmem3) {
		return String.format(
				"A:%02X F:%02X B:%02X C:%02X D:%02X E:%02X H:%02X L:%02X SP:%04X PC:%04X PCMEM:%02X,%02X,%02X,%02X",
				a, f, b, c, d, e, h, l, sp, pc, pcmem0, pcmem1, pcmem2, pcmem3);
	}

	/**
	 * Outputs a trace line to stdout with the current CPU state BEFORE instruction execution.
	 * Reads register values from CPU and 4 bytes of PCMEM from MMU.
	 */
	public void traceLine(Z80Cpu cpu, MMU mmu) {
		int pc = cpu.PC.getValue();
		int pcmem0 = safeReadByte(mmu, pc);
		int pcmem1 = safeReadByte(mmu, pc + 1);
		int pcmem2 = safeReadByte(mmu, pc + 2);
		int pcmem3 = safeReadByte(mmu, pc + 3);

		System.out.println(formatLine(
				cpu.AF.getHi(), cpu.AF.getLo(),
				cpu.BC.getHi(), cpu.BC.getLo(),
				cpu.DE.getHi(), cpu.DE.getLo(),
				cpu.HL.getHi(), cpu.HL.getLo(),
				cpu.SP.getValue(), pc,
				pcmem0, pcmem1, pcmem2, pcmem3));
	}

	private int safeReadByte(MMU mmu, int address) {
		if (address > 0xFFFF) {
			return 0x00;
		}
		return mmu.getByte(address);
	}
}
