package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_E extends OperationINC_r {

	OperationINC_E(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x1c, cpu.DE, true);
	}

}
