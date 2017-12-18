package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationJP extends Operation {

	OperationJP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0xc3, 1);
	}

	@Override
	void execute() {
		int value = mmu.getWord(cpu.PC.getValue() + 1);
		System.out.println("jp " + Integer.toHexString(value));
		cpu.PC.setValue(value);
	}
}
