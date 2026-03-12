package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCP_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationCP_r(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo) {
		super(cpu, mmu, 1, 4, code, 1);
		this.register = register;
		this.lo = lo;
	}

	// Flags: Z=* N=1 H=* C=*
	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		cpu.alu.cpHI(cpu.AF, value);
	}
}
