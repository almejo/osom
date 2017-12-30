package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_E extends OperationLD_A_r {

	OperationLD_A_E(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.DE, true, 1, 4, 0x7b, 1);
	}
}
