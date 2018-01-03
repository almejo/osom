package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPUSH_DE extends OperationPUSH_rr {

	OperationPUSH_DE(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, 1, 16, 0xd5);
	}
}
