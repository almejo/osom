package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationEI extends Operation {

	OperationEI(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xfb, 1);
	}

	@Override
	void execute() {
		if (debug) {
			System.out.println("EI");
		}
		cpu.setInterruptionsEnabled(true);
	}
}
