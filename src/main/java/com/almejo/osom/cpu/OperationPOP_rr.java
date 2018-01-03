package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationPOP_rr extends Operation {

	private final Register register;

	OperationPOP_rr(Z80Cpu cpu, MMU mmu, Register register, Integer m, Integer t, Integer code) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
	}

	@Override
	void execute() {
		if (debug) {
			System.out.println("POP " + register.getName());
		}
		register.setValue(cpu.popWordOnStack());
	}
}
