package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSRL_r extends OperationCB {

	private final Register register;
	private final boolean lo;

	OperationSRL_r(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo) {
		super(cpu, mmu, 2, 8, code, 1);
		this.register = register;
		this.lo = lo;
	}

	// Flags: Z=* N=0 H=0 C=*
	@Override
	void execute() {
		int oldValue = lo ? register.getLo() : register.getHi();
		int result = (oldValue >> 1) & 0xFF;
		register.set(lo, result);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, (oldValue & 1) == 1);
	}
}
