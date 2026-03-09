package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationADD_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationADD_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		int n = lo ? register.getLo() : register.getHi();
		cpu.alu.addRegisterHI(cpu.AF, n);
	}
}
