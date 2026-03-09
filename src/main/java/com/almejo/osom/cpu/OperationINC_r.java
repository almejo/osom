package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationINC_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		if (lo) {
			cpu.alu.incLO(register, true);
		} else {
			cpu.alu.incHI(register, true);
		}
	}
}
