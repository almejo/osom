package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationXOR_r(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo) {
		super(cpu, mmu, 1, 4, code, 1);
		this.register = register;
		this.lo = lo;
	}

	// Flags: Z=* N=0 H=0 C=0
	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		cpu.AF.setHi(cpu.alu.xor(cpu.AF.getHi(), value));
	}
}
