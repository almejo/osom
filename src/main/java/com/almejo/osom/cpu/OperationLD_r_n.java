package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_r_n extends Operation {

	private final boolean lo;
	private Register register;

	OperationLD_r_n(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {

		super(cpu, mmu, m, t, code, 2);
		this.lo = lo;
		this.register = register;
	}

	@Override
	void execute() {
		int address = cpu.PC.getValue() + 1;
		int value = mmu.getByte(address);
		if (debug) {
			printByte("LD " + register.getName(lo) + "," + value, value);
		}
		if (this.lo) {
			register.setLo(mmu.getByte(address));
		} else {
			register.setHi(mmu.getByte(address));
		}
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LD " + register.getName(lo) + ", " + BitUtils.toHex2(ram[base + 1]);
	}
}
