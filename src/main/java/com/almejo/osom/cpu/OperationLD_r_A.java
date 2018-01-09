package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_r_A extends Operation {

	private final Register register;
	private final boolean lo;

	OperationLD_r_A(Z80Cpu cpu, MMU mmu, Register register, boolean lo, Integer m, Integer t, Integer code, Integer length) {
		super(cpu, mmu, m, t, code, length);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		register.set(lo, cpu.AF.getHi());
		if (debug) {
			print("LD " + register.getName(lo) + ", A");
		}

	}
}
