package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRES_0_A extends OperationRES_n_r {
	OperationRES_0_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x87, cpu.AF, false, 0);
	}
}
