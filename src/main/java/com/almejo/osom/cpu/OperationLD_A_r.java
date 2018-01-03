package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationLD_A_r(Z80Cpu cpu, MMU mmu, Register register, boolean lo, Integer m, Integer t, Integer code, Integer length) {
		super(cpu, mmu, m, t, code, length);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		cpu.AF.setHi(lo ? register.getLo() : register.getHi());
		if (debug) {
			System.out.println("LD A, " + register.getName(lo));
		}
	}
}
