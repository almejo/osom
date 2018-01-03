package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPOP_HL extends OperationPOP_rr {

	OperationPOP_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.HL, 1, 12, 0xe1);
	}
}
