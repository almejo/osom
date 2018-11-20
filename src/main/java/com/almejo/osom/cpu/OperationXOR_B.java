package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_B extends OperationXOR_r {

	OperationXOR_B(Z80Cpu cpu, MMU mmu) {

		super(cpu, mmu,  cpu.BC, false, 0xa8);

	}

}
