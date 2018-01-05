package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_D extends OperationDEC_r {
	OperationDEC_D(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x15, cpu.DE, false);
	}
}
