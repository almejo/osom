package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_L_n extends OperationLD_r_n {

	OperationLD_L_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x2e, cpu.HL, true);
	}
}
