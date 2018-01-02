package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCP_HL extends Operation {

	OperationCP_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0xbe, 1);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.HL.getValue());
		System.out.println("CP HL; // 0x" + Integer.toHexString(value));
		cpu.alu.cpHI(cpu.AF, value);
	}
}
