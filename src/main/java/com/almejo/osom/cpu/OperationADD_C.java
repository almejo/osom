package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_C extends OperationADD_r {

	OperationADD_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x81, cpu.BC, true);
	}
}
