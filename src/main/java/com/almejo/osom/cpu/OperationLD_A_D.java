package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_D extends OperationLD_A_r {
	OperationLD_A_D(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, true, 1, 4, 0x79, 1);
	}
}
