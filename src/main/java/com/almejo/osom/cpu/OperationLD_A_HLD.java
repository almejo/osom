package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_HLD extends Operation {

	OperationLD_A_HLD(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x3A, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.AF.setHi(mmu.getByte(cpu.HL.getValue()));
		cpu.HL.setValue(cpu.HL.getValue() - 1);
	}
}
