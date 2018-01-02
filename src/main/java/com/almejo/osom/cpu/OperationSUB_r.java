package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSUB_r extends Operation {
	private Register register;
	private boolean lo;

	OperationSUB_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		System.out.println("SUB " + register.getName(lo));
		cpu.alu.subHI(cpu.AF, lo ? register.getLo() : register.getHi());
	}
}
