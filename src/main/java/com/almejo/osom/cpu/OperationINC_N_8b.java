package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_N_8b extends OperationDEC_N {

	private Register register;
	private boolean lo;

	OperationINC_N_8b(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo) {
		super(cpu, mmu, code, cpu.BC, true);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		System.out.println("INC " + register.getName(lo));
		if (lo) {
			cpu.alu.incLO(register, true);
		} else {
			cpu.alu.incHI(register, true);
		}
	}
}
