package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_aHL extends Operation {

	OperationADD_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x86,1);
	}

	@Override
	void execute() {
		int n = mmu.getByte(cpu.HL.getValue());
		System.out.println("ADD A, [HL]; " + Integer.toHexString(n));
		cpu.alu.addRegisterHI(cpu.AF, n);
	}
}
