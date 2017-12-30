package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_A extends OperationDEC_N {

	OperationDEC_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x3d, cpu.AF, false);
	}
}
