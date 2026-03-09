package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_r_n extends Operation {

	private final boolean lo;
	private final Register register;

	OperationLD_r_n(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {

		super(cpu, mmu, m, t, code, 2);
		this.lo = lo;
		this.register = register;
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		if (this.lo) {
			register.setLo(value);
		} else {
			register.setHi(value);
		}
	}
}
