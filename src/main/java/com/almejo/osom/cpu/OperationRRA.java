package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRRA extends Operation {

	OperationRRA(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x1F, 1);
	}

	// Flags: Z=0 N=0 H=0 C=*
	@Override
	void execute() {
		int oldValue = cpu.AF.getHi();
		int oldCarry = cpu.isFlagSetted(Z80Cpu.FLAG_CARRY) ? 1 : 0;
		int result = ((oldValue >> 1) | (oldCarry << 7)) & 0xFF;
		cpu.AF.setHi(result);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, (oldValue & 1) == 1);
	}
}
