package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRET_Z extends OperationConditional {

	OperationRET_Z(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 20, 8, 0xc8, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		this.actionTaken = false;
		if (cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)) {
			cpu.PC.setValue(cpu.popWordOnStack());
			this.actionTaken = true;
		}
	}
}
