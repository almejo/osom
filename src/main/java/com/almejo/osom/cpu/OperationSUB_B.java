package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSUB_B extends OperationSUB_r {

	OperationSUB_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x90, cpu.BC, false);
	}
}
