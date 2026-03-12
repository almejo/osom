package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_BC_A extends Operation {

	OperationLD_BC_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x02, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		mmu.setByte(cpu.BC.getValue(), cpu.AF.getHi());
	}
}
