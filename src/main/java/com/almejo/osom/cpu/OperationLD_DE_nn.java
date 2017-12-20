package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_DE_nn extends Operation {

	OperationLD_DE_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 12, 0x11, 3);
	}

	@Override
	void execute() {
		int nn = mmu.getWord(cpu.PC.getValue() + 1);
		System.out.println("LD " + cpu.DE.getName() + ", 0x" + Integer.toHexString(nn));
		cpu.DE.setValue(nn);
	}
}
