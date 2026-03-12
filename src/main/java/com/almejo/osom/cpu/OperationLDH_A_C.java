package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_A_C extends Operation {

	OperationLDH_A_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0xF2, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.AF.setHi(mmu.getByte(0xFF00 + cpu.BC.getLo()));
	}
}
