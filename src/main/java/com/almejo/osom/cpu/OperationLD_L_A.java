package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

public class OperationLD_L_A extends UnimplementedOperation {
	public OperationLD_L_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x6f, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LD L, A";
	}
}
