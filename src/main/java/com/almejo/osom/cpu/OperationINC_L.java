package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_L extends OperationINC_r {

	OperationINC_L(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x2c, cpu.HL, true);
	}

}
