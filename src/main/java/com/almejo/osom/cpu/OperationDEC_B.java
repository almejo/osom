package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_B extends OperationDEC_r {

	OperationDEC_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x5, cpu.BC, false);
	}
}
