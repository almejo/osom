package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCP_B extends OperationCP_r {
	OperationCP_B(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.BC, false, 0xb8);
	}
}
