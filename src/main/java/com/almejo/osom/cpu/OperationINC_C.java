package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_C extends OperationINC_N_8b {

	OperationINC_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xc, cpu.BC, true);
	}

}
