package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_A extends Operation {

	OperationXOR_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xaf, 1);
	}

	@Override
	void execute() {
		if (debug) {
			System.out.println("XOR A");
		}
		cpu.AF.setHi(cpu.alu.xor(cpu.AF.getHi(), cpu.AF.getHi()));
	}
}
