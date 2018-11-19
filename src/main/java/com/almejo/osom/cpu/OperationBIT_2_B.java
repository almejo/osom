package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_2_B extends OperationBIT_b_r {

	OperationBIT_2_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x50, 1, cpu.BC, 2, false);
	}
}
