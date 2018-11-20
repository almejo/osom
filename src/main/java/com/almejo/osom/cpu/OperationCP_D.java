package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCP_D extends OperationCP_r {
	OperationCP_D(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.DE, false, 0xba);
	}
}
