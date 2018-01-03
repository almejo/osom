package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationJR_cc_n extends OperationConditional {

	OperationJR_cc_n(Z80Cpu cpu, MMU mmu, int m, int t, int t2, int code, int length) {
		super(cpu, mmu, m, t, t2, code, length);
	}

	@Override
	void execute() {
		this.actionTaken = false;
		int delta = toSignedByte(mmu.getByte(cpu.PC.getValue() + 1));
		if (debug) {
			System.out.println("JR NZ, " + Integer.toHexString(delta));
		}
		if (shouldJump()) {
			cpu.PC.setValue(cpu.PC.getValue() + (delta < 0 ? 1 + delta : 2 + delta));
			this.actionTaken = true;
		}
	}

	abstract boolean shouldJump();


}