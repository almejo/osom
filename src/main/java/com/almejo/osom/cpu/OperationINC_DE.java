package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_DE extends OperationINC_rr {

	OperationINC_DE(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x13, cpu.DE);
	}
}
