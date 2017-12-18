package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_C_n extends OperationLD_N_n {

	OperationLD_C_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0xe, cpu.BC, true);
	}
}
