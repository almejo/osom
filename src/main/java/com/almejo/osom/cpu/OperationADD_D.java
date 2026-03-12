package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_D extends OperationADD_r {

	OperationADD_D(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x82, cpu.DE, false);
	}
}
