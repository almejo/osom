package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_aHL extends Operation {

	OperationDEC_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 12, 0x35, 1);
	}

	@Override
	void execute() {
		int address = cpu.HL.getValue();
		int value = mmu.getByte(address);
		int newValue = (value - 1) & 0xff;
		mmu.setByte(address, newValue);
		cpu.alu.updateDecFlags(value, 1);
	}
}
