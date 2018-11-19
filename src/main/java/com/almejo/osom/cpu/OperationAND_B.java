package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationAND_B extends OperationAND_r {

	OperationAND_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xa0, cpu.BC, false);
	}
}
