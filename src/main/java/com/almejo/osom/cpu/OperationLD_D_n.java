package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_D_n extends OperationLD_r_n {
	OperationLD_D_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x16, cpu.DE, false);
	}
}
