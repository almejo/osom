package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_E_A extends OperationLD_r_A {
	OperationLD_E_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.DE, true, 1, 4, 0x5f, 1);
	}
}
