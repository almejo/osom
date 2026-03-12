package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_C_aHL extends OperationLD_r_aHL {
	OperationLD_C_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x4E, cpu.BC, true);
	}
}
