package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationAND_A extends OperationAND_r {

	OperationAND_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xa7, cpu.AF, false);
	}
}
