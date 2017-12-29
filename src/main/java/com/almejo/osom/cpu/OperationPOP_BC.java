package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPOP_BC extends OperationPOP_rr {

	OperationPOP_BC(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, 1, 12, 0xc1);
	}
}
