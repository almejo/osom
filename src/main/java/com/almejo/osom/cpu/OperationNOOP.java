package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationNOOP extends Operation {

	OperationNOOP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x0, 1);
	}

	@Override
	void execute() {
		if (debug) {
			print("NOP");
		}
	}

	public String decoded(int[] ram, int base){
		return ".db 00";
	}
}
