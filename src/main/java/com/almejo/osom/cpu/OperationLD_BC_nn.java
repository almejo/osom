package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_BC_nn extends Operation {

	OperationLD_BC_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 12, 0x1, 3);
	}

	@Override
	void execute() {
		int value = mmu.getWord(cpu.PC.getValue() + 1);
		if (debug) {
			print("LD " + cpu.BC.getName() + ", 0x" + Integer.toHexString(value));
		}
		cpu.BC.setValue(value);
	}
}
