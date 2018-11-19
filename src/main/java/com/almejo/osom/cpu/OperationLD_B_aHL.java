package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

public class OperationLD_B_aHL extends UnimplementedOperation {
	public OperationLD_B_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x46, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LD B, [HL]";
	}
}
