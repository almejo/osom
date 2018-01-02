package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_B extends OperationLD_A_r {

	OperationLD_A_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, false, 1, 4, 0x78, 1);
	}
}
