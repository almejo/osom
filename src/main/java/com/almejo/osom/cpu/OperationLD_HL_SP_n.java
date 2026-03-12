package com.almejo.osom.cpu;

import static com.almejo.osom.cpu.BitUtils.toSignedByte;

import com.almejo.osom.memory.MMU;

class OperationLD_HL_SP_n extends Operation {

	OperationLD_HL_SP_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 12, 0xF8, 2);
	}

	// Flags: Z=0 N=0 H=* C=*
	@Override
	void execute() {
		int n = toSignedByte(mmu.getByte(cpu.PC.getValue() + 1));
		int sp = cpu.SP.getValue();
		int result = sp + n;
		cpu.setFlag(Z80Cpu.FLAG_ZERO, false);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, ((sp ^ n ^ result) & 0x10) != 0);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, ((sp ^ n ^ result) & 0x100) != 0);
		cpu.HL.setValue(result & 0xFFFF);
	}
}
