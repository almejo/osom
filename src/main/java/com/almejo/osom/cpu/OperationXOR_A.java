package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_A extends OperationXOR_r {

	OperationXOR_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu,  cpu.AF, false, 0xaf);
	}

}
