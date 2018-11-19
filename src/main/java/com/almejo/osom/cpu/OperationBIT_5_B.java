package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_5_B extends OperationBIT_b_r {

	OperationBIT_5_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x68, 1, cpu.BC, 5, false);
	}
}
