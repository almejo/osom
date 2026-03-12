package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSUB_n extends Operation {

	OperationSUB_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xD6, 2);
	}

	// Flags: Z=* N=1 H=* C=*
	@Override
	void execute() {
		int n = mmu.getByte(cpu.PC.getValue() + 1);
		cpu.alu.subHI(cpu.AF, n);
	}
}
