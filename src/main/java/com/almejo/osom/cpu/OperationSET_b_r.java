package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSET_b_r extends OperationCB {

	private final Register register;
	private final boolean lo;
	private final int bit;

	OperationSET_b_r(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo, int bit) {
		super(cpu, mmu, 2, 8, code, 1);
		this.register = register;
		this.lo = lo;
		this.bit = bit;
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		int oldValue = lo ? register.getLo() : register.getHi();
		int value = BitUtils.setBit(oldValue, bit);
		register.set(lo, value);
	}
}
