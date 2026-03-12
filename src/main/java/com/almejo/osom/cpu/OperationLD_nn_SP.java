package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_nn_SP extends Operation {

	OperationLD_nn_SP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 20, 0x08, 3);
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		int address = mmu.getWord(cpu.PC.getValue() + 1);
		mmu.setWord(address, cpu.SP.getValue());
	}
}
