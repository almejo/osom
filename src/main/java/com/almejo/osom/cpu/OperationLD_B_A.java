package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_B_A extends OperationLD_r_A {
	OperationLD_B_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, false, 1, 4, 0x47, 1);
	}
}
