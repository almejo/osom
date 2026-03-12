package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_n extends Operation {

	OperationXOR_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xEE, 2);
	}

	// Flags: Z=* N=0 H=0 C=0
	@Override
	void execute() {
		int n = mmu.getByte(cpu.PC.getValue() + 1);
		cpu.AF.setHi(cpu.alu.xor(cpu.AF.getHi(), n));
	}
}
