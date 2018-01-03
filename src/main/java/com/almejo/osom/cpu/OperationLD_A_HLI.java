package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_HLI extends Operation {

	OperationLD_A_HLI(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x2a, 1);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.HL.getValue());
		if (debug) {
			System.out.println("LD A, [" + cpu.HL.getName() + "I] ; 0x" + Integer.toHexString(value));
		}
		cpu.AF.setHi(value);
		cpu.HL.inc(1);
	}
}
