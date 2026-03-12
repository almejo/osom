package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRST_20 extends OperationRST_n {
	OperationRST_20(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 16, 0xE7, 0x20);
	}
}
