package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRET extends Operation {

	OperationRET(Z80Cpu cpu, MMU mmu) {

		super(cpu, mmu, 1, 16, 0xc9, 1);
	}

	@Override
	void execute() {
		cpu.PC.setValue(cpu.popWordOnStack());
		if (debug) {
			print("RET ;" + cpu.PC);
		}
	}
}
