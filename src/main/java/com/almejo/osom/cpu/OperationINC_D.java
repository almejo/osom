package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_D extends Operation {

	OperationINC_D(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x14, 1);
	}

	// Flags: Z=* N=0 H=* C=-
	@Override
	void execute() {
		cpu.alu.incHI(cpu.DE, true);
	}
}
