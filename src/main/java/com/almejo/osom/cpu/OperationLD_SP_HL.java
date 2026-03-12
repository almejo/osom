package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_SP_HL extends Operation {

	OperationLD_SP_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0xF9, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		cpu.SP.setValue(cpu.HL.getValue());
	}
}
