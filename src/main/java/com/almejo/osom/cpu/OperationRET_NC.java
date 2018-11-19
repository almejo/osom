package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRET_NC extends UnimplementedOperation {

	OperationRET_NC(Z80Cpu cpu, MMU mmu) {

		super(cpu, mmu,  0xd0, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "RET NC";
	}
}
