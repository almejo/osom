package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADC_A_n extends Operation {

	OperationADC_A_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xCE, 2);
	}

	// Flags: Z=* N=0 H=* C=*
	@Override
	void execute() {
		int n = mmu.getByte(cpu.PC.getValue() + 1);
		cpu.alu.adcRegisterHI(cpu.AF, n);
	}
}
