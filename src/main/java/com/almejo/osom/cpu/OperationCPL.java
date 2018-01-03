package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCPL extends Operation {
	OperationCPL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x2f, 1);
	}

	@Override
	void execute() {
		if (debug) {
			System.out.println("CPL");
		}
		cpu.AF.setHi((~cpu.AF.getHi()) & 0xFF);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true);
	}
}
