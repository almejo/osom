package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_0_C extends OperationBIT_b_r {

	OperationBIT_0_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x41, 1, cpu.BC, 0, true);
	}
}
