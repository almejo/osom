package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_B extends OperationINC_r {

	OperationINC_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x4, cpu.BC, false);
	}
}
