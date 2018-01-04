package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPUSH_AF extends OperationPUSH_rr {

	OperationPUSH_AF(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.AF, 1, 16, 0xf5);
	}
}
