package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRST_30 extends OperationRST_n {
	OperationRST_30(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 16, 0xF7, 0x30);
	}
}
