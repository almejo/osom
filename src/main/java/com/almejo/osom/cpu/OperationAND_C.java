package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationAND_C extends OperationAND_r {

	OperationAND_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xa1, cpu.BC, true);
	}
}
