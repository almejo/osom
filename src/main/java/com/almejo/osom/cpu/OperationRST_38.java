package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRST_38 extends OperationRST_n {
	OperationRST_38(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 16, 0xFF, 0x38);
	}
}
