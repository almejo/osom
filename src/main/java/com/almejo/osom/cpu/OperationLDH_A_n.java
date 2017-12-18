package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_A_n extends Operation {
	OperationLDH_A_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0xf0, 2);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		System.out.println("LD A, (0xFF00 + " + Integer.toHexString(value) + ")");
		cpu.AF.setHi(mmu.getByte(0xFF00 + value));
	}
}
