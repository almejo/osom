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
			print("CP 0x" + Integer.toHexString(n) + " ; " + Integer.toHexString(cpu.AF.getHi()) + " == " + Integer.toHexString(n));
		}
		cpu.alu.cpHI(cpu.AF, n);
	}
}
