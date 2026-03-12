package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_L extends Operation {

	OperationDEC_L(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x2D, 1);
	}

	// Flags: Z=* N=1 H=* C=-
	@Override
	void execute() {
		cpu.alu.decLO(cpu.HL);
	}
}
