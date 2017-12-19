package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_N extends Operation {
	private Register register;
	private boolean lo;

	OperationDEC_N(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo) {
		super(cpu, mmu, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		System.out.println("DEC " + register.getName(lo));
		if (lo) {
			cpu.alu.decLO(register, true);
		} else {
			cpu.alu.decHI(register, true);
		}
	}
}
