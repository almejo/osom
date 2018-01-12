package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCP_n extends Operation {

	OperationCP_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xfe, 2);
	}

	@Override
	void execute() {
		int n = mmu.getByte(cpu.PC.getValue() + 1);
		if (debug) {
			printByte("CP " + n, n);
		}
		cpu.alu.cpHI(cpu.AF, n);
	}
}
