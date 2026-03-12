package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRST_00 extends OperationRST_n {
	OperationRST_00(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 16, 0xC7, 0x00);
	}
}
