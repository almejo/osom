package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSCF extends Operation {

	OperationSCF(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x37, 1);
	}

	// Flags: Z=- N=0 H=0 C=1
	@Override
	void execute() {
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, true);
	}
}
