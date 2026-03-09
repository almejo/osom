package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_aHL extends Operation {

	OperationINC_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 12, 0x34, 1);
	}

	void execute() {
		int address = cpu.HL.getValue();
		int value = mmu.getByte(address);
		int newValue = (value + 1) & 0xff;
		mmu.setByte(address, newValue);
		cpu.alu.updateIncFlags(value, 1);
	}

}
