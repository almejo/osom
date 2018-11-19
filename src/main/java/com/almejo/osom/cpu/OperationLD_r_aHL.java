package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract  class OperationLD_r_aHL extends Operation {

	private final boolean lo;
	private Register register;

	OperationLD_r_aHL(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.lo = lo;
		this.register = register;
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.HL.getValue());
		if (debug) {
			print("LD " + register.getName(lo) + ", [HL] ; " +  Integer.toHexString(value));
		}
		if (this.lo) {
			register.setLo(value);
		} else {
			register.setHi(value);
		}
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LD " + register.getName(lo) + ", [HL] ";
	}
}
