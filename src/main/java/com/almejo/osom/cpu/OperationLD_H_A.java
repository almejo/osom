package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_H_A extends OperationLD_r_A {
	OperationLD_H_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, cpu.HL, false, 1, 4, 0x67, 1);
	}
}
