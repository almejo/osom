package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_E_aHL extends OperationLD_r_aHL{
	OperationLD_E_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x5e, cpu.DE, true);
	}
}
