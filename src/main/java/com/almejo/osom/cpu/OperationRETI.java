package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRETI extends Operation {

	OperationRETI(Z80Cpu cpu, MMU mmu) {

		super(cpu, mmu, 1, 16, 0xd9, 1);
	}

	@Override
	void execute() {
		cpu.PC.setValue(cpu.popWordOnStack());
		cpu.setInterruptionsEnabled(true);
	}
}
