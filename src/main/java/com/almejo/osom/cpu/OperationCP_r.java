package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationCP_r extends UnimplementedOperation {

	private final Register register;
	private final boolean lo;

	OperationCP_r(Z80Cpu cpu, MMU mmu, Register register, boolean lo, int code) {
		super(cpu, mmu, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "CP " + register.getName(lo);
	}
}
