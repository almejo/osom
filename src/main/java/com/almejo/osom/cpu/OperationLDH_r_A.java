package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_r_A extends Operation {
	private final Register register;
	private final boolean lo;

	OperationLDH_r_A(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		mmu.setByte(0xFF00 + value, cpu.AF.getHi());
	}
}
