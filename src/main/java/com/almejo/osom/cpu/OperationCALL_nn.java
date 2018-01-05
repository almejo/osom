package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCALL_nn extends Operation {

	OperationCALL_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 24, 0xcd, 3);
	}

	@Override
	void execute() {
		int nn = mmu.getWord(cpu.PC.getValue() + 1);
		if (debug) {
			print("CALL 0x" + Integer.toHexString(nn));
		}
		cpu.pushWordOnStack(cpu.PC.getValue() + getLength());
		cpu.PC.setValue(nn);
	}
}
