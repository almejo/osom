package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRL_r extends OperationCB {
	private Register register;
	private boolean lo;

	OperationRL_r(Z80Cpu cpu, MMU mmu, Register register, boolean lo, int m, int t, int code) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		int oldValue = lo ? register.getLo() : register.getHi();
		int value = cpu.alu.rotateLeft(oldValue);
		register.set(lo, value);
		System.out.println("RL " + register.getName(lo) + "; //  0x" + Integer.toHexString(oldValue) + "--> 0x" + Integer.toHexString(value));
	}
}
