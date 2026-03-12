package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSBC_A_n extends Operation {

	OperationSBC_A_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xDE, 2);
	}

	// Flags: Z=* N=1 H=* C=*
	@Override
	void execute() {
		int n = mmu.getByte(cpu.PC.getValue() + 1);
		cpu.alu.sbcRegisterHI(cpu.AF, n);
	}
}
