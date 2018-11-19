package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

public class OperationLD_L_E extends UnimplementedOperation {
	public OperationLD_L_E(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x6b, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LD L, E";
	}
}
