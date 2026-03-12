package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_DE extends Operation {

	OperationDEC_DE(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x1B, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.alu.dec(cpu.DE, false);
	}
}
