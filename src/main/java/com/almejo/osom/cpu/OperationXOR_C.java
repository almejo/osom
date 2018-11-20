package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_C extends OperationXOR_r {

	OperationXOR_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu,  cpu.BC, true, 0xa9);
	}
}
