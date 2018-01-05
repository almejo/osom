package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_r extends Operation {
	private Register register;
	private boolean lo;

	OperationDEC_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		if (debug) {
			print("DEC " + register.getName(lo));
		}
		if (lo) {
			cpu.alu.decLO(register);
		} else {
			cpu.alu.decHI(register);
		}
	}
}
