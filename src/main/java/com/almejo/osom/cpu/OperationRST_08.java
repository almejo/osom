package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRST_08 extends OperationRST_n {
	OperationRST_08(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 16, 0xCF, 0x08);
	}
}
