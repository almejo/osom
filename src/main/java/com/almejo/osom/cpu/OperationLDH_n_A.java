package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDH_n_A extends Operation {
	OperationLDH_n_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 12, 0xe0, 2);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		if (debug) {
			printByte("LD (FF" + BitUtils.toHex2(value) + "h),a", value);
		}
		mmu.setByte(0xFF00 + value, cpu.AF.getHi());
	}
}
