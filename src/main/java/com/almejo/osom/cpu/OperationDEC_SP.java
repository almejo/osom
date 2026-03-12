package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_SP extends Operation {

	OperationDEC_SP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x3B, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.SP.setValue(cpu.SP.getValue() - 1);
	}
}
