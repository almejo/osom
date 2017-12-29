package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_C_A extends OperationLDH_r_A {

	OperationLDH_C_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xe2, cpu.BC, true);
	}
}
