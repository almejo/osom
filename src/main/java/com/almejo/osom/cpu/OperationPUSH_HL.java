package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPUSH_HL extends OperationPUSH_rr {

	OperationPUSH_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.HL, 1, 16, 0xe5);
	}
}
