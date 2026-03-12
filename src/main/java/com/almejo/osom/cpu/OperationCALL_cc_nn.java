package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationCALL_cc_nn extends OperationConditional {

	OperationCALL_cc_nn(Z80Cpu cpu, MMU mmu, int t, int t2, int code) {
		super(cpu, mmu, 3, t, t2, code, 3);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		this.actionTaken = false;
		int address = mmu.getWord(cpu.PC.getValue() + 1);
		if (shouldCall()) {
			cpu.pushWordOnStack(cpu.PC.getValue() + getLength());
			cpu.PC.setValue(address);
			this.actionTaken = true;
		}
	}

	abstract boolean shouldCall();
}
