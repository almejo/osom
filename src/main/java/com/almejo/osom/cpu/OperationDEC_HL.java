package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_HL extends Operation {

	OperationDEC_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x2B, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.alu.dec(cpu.HL, false);
	}
}
