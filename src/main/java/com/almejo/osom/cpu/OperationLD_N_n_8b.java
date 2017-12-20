package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_N_n_8b extends Operation {

	private final boolean lo;
	private Register register;

	OperationLD_N_n_8b(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {

		super(cpu, mmu, m, t, code, 2);
		this.lo = lo;
		this.register = register;
	}

	@Override
	void execute() {
		int address = cpu.PC.getValue() + 1;
		System.out.println("LD " + register.getName(lo) + ", 0x" + Integer.toHexString(mmu.getByte(address)));
		if (this.lo) {
			register.setLo(mmu.getByte(address));
		} else {
			register.setHi(mmu.getByte(address));
		}
	}
}
