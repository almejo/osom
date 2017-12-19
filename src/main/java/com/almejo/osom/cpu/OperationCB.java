package com.almejo.osom.cpu;


import com.almejo.osom.memory.MMU;

abstract class OperationCB extends Operation {

	OperationCB(Z80Cpu cpu, MMU mmu, int m, int t, int code, int length) {
		super(cpu, mmu, m, t, code, length);
	}
}
