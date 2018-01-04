package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPOP_AF extends OperationPOP_rr {

	OperationPOP_AF(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.AF, 1, 12, 0xf1);
	}
}
