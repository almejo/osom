package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_4_B extends OperationBIT_b_r {

	OperationBIT_4_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x60, 1, cpu.BC, 4, false);
	}
}
