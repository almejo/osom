package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSWAP_r extends OperationCB {

	private Register register;
	private boolean lo;

	OperationSWAP_r(Z80Cpu cpu, MMU mmu, int m, int t, int code, int length, Register register, boolean lo) {
		super(cpu, mmu, m, t, code, length);
		this.register = register;
		this.lo = lo;
	}

	@Override
	void execute() {
		int value = lo ? register.getLo() : register.getHi();
		value = ((value & 0xF0) >> 4) | ((value & 0x0F) << 4);
		if (debug) {
			print("SWAP " + register.getName(lo));
		}
		cpu.AF.setHi(value);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, value == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
	}
}
