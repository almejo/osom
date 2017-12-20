package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_N_A extends Operation {
	private Register register;
	private boolean lo;

	OperationLDH_N_A(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		System.out.println("LD (0xFF00 + " + register.getName(lo) + "), A; 0x" + Integer.toHexString(0xff00 + value) + " <- 0x" + Integer.toHexString(cpu.AF.getHi()));
		mmu.setByte(0xFF00 + value, cpu.AF.getHi());
	}
}
