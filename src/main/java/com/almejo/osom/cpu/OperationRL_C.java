package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRL_C extends OperationRL_r {

	OperationRL_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, true, 2, 8, 0x11);
	}

}
