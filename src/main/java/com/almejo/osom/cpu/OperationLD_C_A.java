package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_C_A extends OperationLD_r_A {
	OperationLD_C_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, true, 1, 4, 0x4f, 1);
	}
}
