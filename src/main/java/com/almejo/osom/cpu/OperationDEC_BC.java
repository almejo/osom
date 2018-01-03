package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_BC extends OperationDEC_rr {
	OperationDEC_BC(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0xb, cpu.BC);
	}
}
