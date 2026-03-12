package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_aHL extends Operation {

	OperationXOR_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0xAE, 1);
	}

	// Flags: Z=* N=0 H=0 C=0
	@Override
	void execute() {
		int value = mmu.getByte(cpu.HL.getValue());
		cpu.AF.setHi(cpu.alu.xor(cpu.AF.getHi(), value));
	}
}
