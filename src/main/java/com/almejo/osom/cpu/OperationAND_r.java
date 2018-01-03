package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationAND_r extends Operation {

	private Register register;
	private boolean lo;

	OperationAND_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		if (debug) {
			System.out.println("AND " + register.getName(lo));
		}
		int result = (cpu.AF.getHi() & value) & 0xff;
		cpu.AF.setHi(result);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);

	}
}
