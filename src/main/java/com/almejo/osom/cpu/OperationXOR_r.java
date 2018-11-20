package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationXOR_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationXOR_r(Z80Cpu cpu, MMU mmu, Register register, boolean lo, int code) {
		super(cpu, mmu, 1, 4, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		if (debug) {
			print("XOR " + register.getName(lo));
		}
		cpu.AF.setHi(cpu.alu.xor(cpu.AF.getHi(), lo ? register.getLo() : register.getHi()));
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "XOR " + register.getName(lo);
	}
}
