package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_DE_A extends Operation {

	OperationLD_DE_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x12, 1);
	}

	@Override
	void execute() {
		int value = cpu.AF.getHi();
		if (debug) {
			print("LD " + cpu.DE.getName() + ", 0x" + Integer.toHexString(value));
		}
		mmu.setByte(cpu.DE.getValue(), value);
	}
}
