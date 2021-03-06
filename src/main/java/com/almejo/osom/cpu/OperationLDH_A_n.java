package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_A_n extends Operation {
	OperationLDH_A_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 12, 0xf0, 2);
	}

	@Override
	void execute() {
		int address = mmu.getByte(cpu.PC.getValue() + 1);
		int value = mmu.getByte(0xFF00 + address);
		if (debug) {
			printByte("LD A,(FF" + BitUtils.toHex2(address) + "h)", address);
		}
		cpu.AF.setHi(value);
	}
}
