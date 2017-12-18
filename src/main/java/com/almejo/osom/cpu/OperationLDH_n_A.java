package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_n_A extends Operation {
	OperationLDH_n_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0xe0, 2);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		System.out.println("LD (0xFF00 + " + Integer.toHexString(value) + "), A");
		mmu.setByte(0xFF + value, cpu.AF.getHi());
	}
}
