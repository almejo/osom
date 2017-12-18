package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_N_n extends Operation {

	private final boolean lo;
	private Register register;

	OperationLD_N_n(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo) {

		super(cpu, mmu, code, 2);
		this.lo = lo;
		this.register = register;
	}

	@Override
	void execute() {
		int address = cpu.PC.getValue() + 1;
		System.out.println("LD " + register.getName().charAt(lo ? 1 : 0) + ", 0x" + Integer.toHexString(mmu.getByte(address)));
		if (this.lo) {
			register.setLo(mmu.getByte(address));
		} else {
			register.setHi(mmu.getByte(address));
		}
	}
}
