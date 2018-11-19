package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_3_B extends OperationBIT_b_r {

	OperationBIT_3_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x58, 1, cpu.BC, 3, false);
	}
}
