package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRET_NC extends OperationConditional {

	OperationRET_NC(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 20, 8, 0xD0, 1);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		this.actionTaken = false;
		if (!cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)) {
			cpu.PC.setValue(cpu.popWordOnStack());
			this.actionTaken = true;
		}
	}
}
