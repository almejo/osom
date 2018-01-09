package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_HL_n extends Operation {

	OperationLD_HL_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 12, 0x36, 2);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		if (debug) {
			printByte("LD (" + cpu.HL.getName() + ")," + value, value);
		}
		mmu.setByte(cpu.HL.getValue(), value);
	}
}
