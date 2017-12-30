package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationJR_NZ_n extends OperationJR_cc_n {

	OperationJR_NZ_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 12, 8, 0x20, 2);
	}

	@Override
	boolean shouldJump() {
		return !cpu.isFlagSetted(Z80Cpu.FLAG_ZERO);
	}
}