package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_E extends OperationDEC_N {
	OperationDEC_E(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x1d, cpu.DE, true);
	}
}
