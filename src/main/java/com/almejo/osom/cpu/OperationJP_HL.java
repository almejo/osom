package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationJP_HL extends Operation {

	OperationJP_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xe9, 1);
	}

	@Override
	void execute() {
		int value = cpu.HL.getValue();
		cpu.PC.setValue(value);
	}
}