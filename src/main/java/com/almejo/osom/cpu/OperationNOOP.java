package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationNOOP extends Operation {

	OperationNOOP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x0, 1);
	}

	@Override
	void execute() {

	}
}
