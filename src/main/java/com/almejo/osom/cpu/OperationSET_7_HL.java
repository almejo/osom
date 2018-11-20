package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSET_7_HL extends UnimplementedOperationCB {
	OperationSET_7_HL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0xfe, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "SET 7, [HL]";
	}
}
