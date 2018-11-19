package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADC_A_aHL extends UnimplementedOperation {

	OperationADC_A_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x27, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "ADC_A_aHL";
	}
}
