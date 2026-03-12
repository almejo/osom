package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_A_n extends Operation {

	OperationADD_A_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xC6, 2);
	}

	// Flags: Z=* N=0 H=* C=*
	@Override
	void execute() {
		int n = mmu.getByte(cpu.PC.getValue() + 1);
		cpu.alu.addRegisterHI(cpu.AF, n);
	}
}
