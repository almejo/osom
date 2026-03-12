package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_L_aHL extends OperationLD_r_aHL {
	OperationLD_L_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x6E, cpu.HL, true);
	}
}
