package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_aHL_r extends Operation {

	private final Register register;
	private final boolean lo;

	OperationLD_aHL_r(Z80Cpu cpu, MMU mmu, int code, Register register, boolean lo) {
		super(cpu, mmu, 1, 8, code, 1);
		this.register = register;
		this.lo = lo;
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		mmu.setByte(cpu.HL.getValue(), value);
	}
}
