package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPOP_DE extends OperationPOP_rr {

	OperationPOP_DE(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.DE, 1, 12, 0xd1);
	}
}
