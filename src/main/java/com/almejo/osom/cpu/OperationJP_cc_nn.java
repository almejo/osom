package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationJP_cc_nn extends OperationConditional {

	OperationJP_cc_nn(Z80Cpu cpu, MMU mmu, int m, int t, int t2, int code, int length) {
		super(cpu, mmu, m, t, t2, code, length);
	}

	@Override
	void execute() {
		this.actionTaken = false;
		int address = mmu.getWord(cpu.PC.getValue() + 1);
		if (shouldJump()) {
			cpu.PC.setValue(address);
			this.actionTaken = true;
		}
	}

	abstract boolean shouldJump();


}