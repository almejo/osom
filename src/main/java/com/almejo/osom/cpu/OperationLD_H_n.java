package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_H_n extends OperationLD_r_n {

	OperationLD_H_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x26, cpu.HL, false);
	}
}
