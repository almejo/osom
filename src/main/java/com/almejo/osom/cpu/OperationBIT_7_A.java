package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_7_A extends OperationBIT_b_r {

	OperationBIT_7_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x7f, 1, cpu.AF, 7, false);
	}
}
