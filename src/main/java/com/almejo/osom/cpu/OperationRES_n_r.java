package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationRES_n_r extends OperationCB {
	private Register register;
	private boolean lo;
	private int bit;

	OperationRES_n_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo, int bit) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
		this.bit = bit;
	}

	@Override
	void execute() {
		int oldValue = lo ? register.getLo() : register.getHi();
		int value = BitUtils.resetBit(oldValue, bit);
		register.set(lo, value);
		if (debug) {
			System.out.println("RES " + bit + ", " + register.getName(lo));
		}
	}
}
