package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_A extends OperationINC_r {
	OperationINC_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x3c, cpu.AF, false);
	}
}
