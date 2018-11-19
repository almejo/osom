package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationEI extends Operation {

	OperationEI(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xfb, 1);
	}

	@Override
	void execute() {
		if (debug) {
			print("EI");
		}
		cpu.setInterruptionsEnabled(true);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "EI";
	}
}
