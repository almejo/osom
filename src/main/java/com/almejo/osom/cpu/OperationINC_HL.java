package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_HL extends OperationINC_rr {

	OperationINC_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x23, cpu.HL);
	}
}
