package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_A extends OperationADD_r {

	OperationADD_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x87, cpu.AF, false);
	}
}
