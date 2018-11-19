package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_0_D extends OperationBIT_b_r {

	OperationBIT_0_D(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x42, 1, cpu.DE, 0, false);
	}

}
