package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_HLI_A extends Operation {

	OperationLD_HLI_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x22, 1);
	}

	@Override
	void execute() {
		int value = cpu.AF.getHi();
		mmu.setByte(cpu.HL.getValue(), value);
		cpu.HL.inc(1);
	}
}
