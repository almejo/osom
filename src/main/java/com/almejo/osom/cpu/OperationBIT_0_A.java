package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_0_A extends OperationBIT_b_r {

	OperationBIT_0_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x47, 1, cpu.AF, 0, false);
	}
}
