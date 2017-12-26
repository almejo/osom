package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDI extends Operation {

	OperationDI(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xf3, 1);
	}

	@Override
	void execute() {
		cpu.setInterruptionsEnabled(false);
	}
}
