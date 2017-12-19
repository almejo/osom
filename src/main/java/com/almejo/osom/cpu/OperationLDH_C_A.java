package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_C_A extends OperationLDH_N_A {

	OperationLDH_C_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0xe2, cpu.BC, true);
	}
}
