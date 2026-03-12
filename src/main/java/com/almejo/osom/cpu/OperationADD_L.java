package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_L extends OperationADD_r {

	OperationADD_L(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x85, cpu.HL, true);
	}
}
