package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_C extends Operation {

	OperationXOR_C(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0xa9, 1);
	}

	@Override
	void execute() {
		if (debug) {
			System.out.println("XOR C");
		}
		cpu.AF.setHi(cpu.alu.xor(cpu.AF.getHi(), cpu.BC.getLo()));
	}
}
