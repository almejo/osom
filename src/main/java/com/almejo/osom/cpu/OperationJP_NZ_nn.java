package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationJP_NZ_nn extends OperationJP_cc_nn {

	OperationJP_NZ_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 16, 12, 0xC2, 3);
	}

	@Override
	boolean shouldJump() {
		return !cpu.isFlagSetted(Z80Cpu.FLAG_ZERO);
	}
}
