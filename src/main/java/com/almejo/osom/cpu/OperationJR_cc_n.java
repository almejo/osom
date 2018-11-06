package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationJR_cc_n extends OperationConditional {

	private String condition;

	OperationJR_cc_n(Z80Cpu cpu, MMU mmu, int m, int t, int t2, String condition, int code, int length) {
		super(cpu, mmu, m, t, t2, code, length);

		this.condition = condition;
	}

	@Override
	void execute() {
		this.actionTaken = false;
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		int delta = toSignedByte(value);
		if (debug) {
			printByte("JR " + condition + "," + toSignedByte(value), value);
		}
		if (shouldJump()) {
			cpu.PC.setValue(cpu.PC.getValue() + (delta < 0 ? 1 + delta : 2 + delta));
			//cpu.PC.setValue(cpu.PC.getValue() + (delta < 0 ? 2 + delta : 2 + delta));
			this.actionTaken = true;
		}
	}

	abstract boolean shouldJump();


}
