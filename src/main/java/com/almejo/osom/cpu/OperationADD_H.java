package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_H extends OperationADD_r {

	OperationADD_H(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x84, cpu.HL, false);
	}
}
