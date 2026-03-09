package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_rr extends Operation {
	private final Register register;

	OperationDEC_rr(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
	}

	@Override
	void execute() {
		cpu.alu.dec(register, false);
	}
}
