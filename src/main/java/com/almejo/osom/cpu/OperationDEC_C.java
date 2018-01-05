package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_C extends OperationDEC_r {

	OperationDEC_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x0d, cpu.BC, true);
	}
}
