package com.almejo.osom.cpu;

import static com.almejo.osom.cpu.BitUtils.toSignedByte;

import com.almejo.osom.memory.MMU;

class OperationADD_SP_n extends Operation {

	OperationADD_SP_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 16, 0xE8, 2);
	}

	// Flags: Z=0 N=0 H=* C=*
	@Override
	void execute() {
		int n = toSignedByte(mmu.getByte(cpu.PC.getValue() + 1));
		cpu.SP.setValue(cpu.alu.addSignedByteToWord(cpu.SP.getValue(), n));
	}
}
