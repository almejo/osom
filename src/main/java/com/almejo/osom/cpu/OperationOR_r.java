package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationOR_r extends Operation {

	private Register register;
	private boolean lo;

	OperationOR_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);

		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		if (debug) {
			System.out.println("OR " + register.getName(lo));
		}
		cpu.AF.setHi(cpu.alu.or(cpu.AF.getHi(), lo ? register.getLo() : register.getHi()));

	}
}
