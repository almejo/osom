package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRST_10 extends OperationRST_n {
	OperationRST_10(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 16, 0xD7, 0x10);
	}
}
