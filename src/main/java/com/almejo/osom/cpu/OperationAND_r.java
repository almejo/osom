package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationAND_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationAND_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	// Flags: Z=* N=0 H=1 C=0
	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		int result = (cpu.AF.getHi() & value) & 0xff;
		cpu.AF.setHi(result);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);

	}
}
