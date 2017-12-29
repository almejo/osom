package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationBIT_b_n extends OperationCB {

	private Register register;
	private int b;
	private boolean lo;

	OperationBIT_b_n(Z80Cpu cpu, MMU mmu, int m, int t, int code, int length, Register register, int b, boolean lo) {
		super(cpu, mmu, m, t, code, length);
		this.register = register;
		this.b = b;
		this.lo = lo;
	}

	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		System.out.println("BIT " + b + ", " + register.getName(lo));
		cpu.alu.setBIT((value & 1 << b) == 0);
	}
}
