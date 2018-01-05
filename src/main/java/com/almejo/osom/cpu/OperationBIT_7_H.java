package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_7_H extends OperationBIT_b_r {

	OperationBIT_7_H(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x7c, 1, cpu.HL, 7, false);
	}

}
