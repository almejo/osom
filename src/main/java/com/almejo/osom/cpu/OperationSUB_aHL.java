package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSUB_aHL extends Operation {

	OperationSUB_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x96, 1);
	}

	// Flags: Z=* N=1 H=* C=*
	@Override
	void execute() {
		int n = mmu.getByte(cpu.HL.getValue());
		cpu.alu.subHI(cpu.AF, n);
	}
}
