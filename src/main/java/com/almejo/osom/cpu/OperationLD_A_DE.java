package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_DE extends Operation {

	OperationLD_A_DE(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x1a, 1);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.DE.getValue());
		System.out.println("LD " + cpu.AF.getName(false) + ", (" + cpu.DE.getName() + ")");
		cpu.AF.setHi(value);
	}
}
