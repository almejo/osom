package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCALL_NZ_nn extends OperationCALL_cc_nn {

	OperationCALL_NZ_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 24, 12, 0xC4);
	}

	@Override
	boolean shouldCall() {
		return !cpu.isFlagSetted(Z80Cpu.FLAG_ZERO);
	}
}
