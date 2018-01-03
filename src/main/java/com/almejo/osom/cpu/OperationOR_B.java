package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationOR_B extends OperationOR_r {
	OperationOR_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xb0, cpu.BC, false);
	}
}
