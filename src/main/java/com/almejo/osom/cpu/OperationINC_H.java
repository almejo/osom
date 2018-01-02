package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_H extends OperationINC_r {

	OperationINC_H(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x24, cpu.HL, false);
	}

}
