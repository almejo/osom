package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRRCA extends Operation {

	OperationRRCA(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x0F, 1);
	}

	// Flags: Z=0 N=0 H=0 C=*
	@Override
	void execute() {
		int oldValue = cpu.AF.getHi();
		int carry = oldValue & 1;
		int result = ((oldValue >> 1) | (carry << 7)) & 0xFF;
		cpu.AF.setHi(result);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, carry == 1);
	}
}
