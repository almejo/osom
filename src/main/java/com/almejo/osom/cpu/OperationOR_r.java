package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationOR_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationOR_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);

		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		cpu.AF.setHi(cpu.alu.or(cpu.AF.getHi(), lo ? register.getLo() : register.getHi()));

	}
}
