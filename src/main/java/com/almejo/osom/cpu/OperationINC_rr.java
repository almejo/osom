package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_rr extends Operation {

	private Register register;

	OperationINC_rr(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
	}

	@Override
	void execute() {
		if (debug) {
			System.out.println("INC " + register.getName());
		}
		register.inc(1);
	}
}
