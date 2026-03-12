package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_SP extends Operation {

	OperationINC_SP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x33, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.SP.setValue(cpu.SP.getValue() + 1);
	}
}
