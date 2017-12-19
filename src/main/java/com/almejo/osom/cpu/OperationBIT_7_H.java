package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_7_H extends OperationBIT_b_n8b {

	OperationBIT_7_H(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x7c, 1, cpu.HL, 7, false);
	}

}
