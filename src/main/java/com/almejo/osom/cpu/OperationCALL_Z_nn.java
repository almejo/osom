package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCALL_Z_nn extends OperationCALL_cc_nn {

	OperationCALL_Z_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 24, 12, 0xCC);
	}

	@Override
	boolean shouldCall() {
		return cpu.isFlagSetted(Z80Cpu.FLAG_ZERO);
	}
}
