package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

public abstract class UnimplementedOperationCB extends OperationCB {
	UnimplementedOperationCB(Z80Cpu cpu, MMU mmu, int code, int length) {
		super(cpu, mmu, 0, 0, code, length);
	}

	@Override
	void execute() {
		throw new UnimplementedOperationException();
	}
}
