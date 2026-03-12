package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationADD_HL_SP extends OperationADD_HL_rr {
	OperationADD_HL_SP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x39, cpu.SP);
	}
}
