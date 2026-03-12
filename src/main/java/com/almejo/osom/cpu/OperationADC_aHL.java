package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADC_aHL extends Operation {

	OperationADC_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x8E, 1);
	}

	// Flags: Z=* N=0 H=* C=*
	@Override
	void execute() {
		int n = mmu.getByte(cpu.HL.getValue());
		cpu.alu.adcRegisterHI(cpu.AF, n);
	}
}
