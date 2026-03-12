package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationAND_aHL extends Operation {

	OperationAND_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0xA6, 1);
	}

	// Flags: Z=* N=0 H=1 C=0
	@Override
	void execute() {
		int value = mmu.getByte(cpu.HL.getValue());
		int result = (cpu.AF.getHi() & value) & 0xFF;
		cpu.AF.setHi(result);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
	}
}
