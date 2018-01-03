package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSWAP_A extends OperationSWAP_r {

	OperationSWAP_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0x37, 1, cpu.AF, false);

	}
}
