package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationJP_Z_nn extends OperationJP_cc_nn {

	OperationJP_Z_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 16, 12, "Z", 0xca, 3);
	}


	@Override
	boolean shouldJump() {
		return cpu.isFlagSetted(Z80Cpu.FLAG_ZERO);
	}
}
