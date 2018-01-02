package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationOR_C extends OperationOR_r {
	OperationOR_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xb1, cpu.BC, true);
	}
}
