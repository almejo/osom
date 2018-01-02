package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_L extends OperationLD_A_r {

	OperationLD_A_L(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.HL, true, 1, 4, 0x7d, 1);
	}
}
