package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_H extends OperationLD_A_r {

	OperationLD_A_H(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.HL, false, 1, 4, 0x7c, 1);
	}
}
