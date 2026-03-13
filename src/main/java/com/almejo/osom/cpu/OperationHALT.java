package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationHALT extends Operation {

	OperationHALT(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x76, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.setHalted(true);
	}
}
