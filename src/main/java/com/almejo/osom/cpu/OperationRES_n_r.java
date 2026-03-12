package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRES_n_r extends OperationCB {
	private final Register register;
	private final boolean lo;
	private final int bit;

	OperationRES_n_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo, int bit) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
		this.bit = bit;
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		int oldValue = lo ? register.getLo() : register.getHi();
		int value = BitUtils.resetBit(oldValue, bit);
		register.set(lo, value);
	}
}
