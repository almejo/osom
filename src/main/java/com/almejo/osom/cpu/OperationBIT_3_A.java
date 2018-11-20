package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_3_A extends OperationBIT_b_r {

	OperationBIT_3_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x5f, 1, cpu.AF, 3, false);
	}
}
