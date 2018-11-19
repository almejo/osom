package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDAA extends UnimplementedOperation {

	OperationDAA(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x8e, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "DAA";
	}
}
