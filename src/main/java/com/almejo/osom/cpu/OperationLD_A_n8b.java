package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_n8b extends OperationLD_r_n_8b {

	OperationLD_A_n8b(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x3e, cpu.AF, false);
	}
}
