package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_B extends OperationADD_r {

	OperationADD_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x80, cpu.BC, false);
	}
}
