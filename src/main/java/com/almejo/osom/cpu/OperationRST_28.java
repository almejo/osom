package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRST_28 extends OperationRST_n {
	OperationRST_28(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 16, 0xef, 0x28);
	}
}
