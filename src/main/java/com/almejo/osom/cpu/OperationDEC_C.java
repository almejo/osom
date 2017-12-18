package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_C extends OperationDEC_N {

	OperationDEC_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x0d, cpu.BC, true);
	}
}
