package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_E extends OperationADD_r {

	OperationADD_E(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x83, cpu.DE, true);
	}
}
