package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_D_A extends OperationLD_r_A {
	OperationLD_D_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.DE, false, 1, 4, 0x57, 1);
	}
}
