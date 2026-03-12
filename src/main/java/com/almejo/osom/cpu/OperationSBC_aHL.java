package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSBC_aHL extends Operation {

	OperationSBC_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x9E, 1);
	}

	// Flags: Z=* N=1 H=* C=*
	@Override
	void execute() {
		int n = mmu.getByte(cpu.HL.getValue());
		cpu.alu.sbcRegisterHI(cpu.AF, n);
	}
}
