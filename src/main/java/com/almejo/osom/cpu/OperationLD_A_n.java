package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_n extends OperationLD_N_n {

	OperationLD_A_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x3e, cpu.AF, false);
	}
}
