package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_H extends Operation {

	OperationDEC_H(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x25, 1);
	}

	// Flags: Z=* N=1 H=* C=-
	@Override
	void execute() {
		cpu.alu.decHI(cpu.HL);
	}
}
