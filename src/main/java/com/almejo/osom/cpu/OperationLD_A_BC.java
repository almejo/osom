package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_BC extends Operation {

	OperationLD_A_BC(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x0A, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		int value = mmu.getByte(cpu.BC.getValue());
		cpu.AF.setHi(value);
	}
}
