package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationConditional extends Operation {
	protected int t2;
	protected boolean actionTaken;

	OperationConditional(Z80Cpu cpu, MMU mmu, int m, int t, int t2, int code, int length) {
		super(cpu, mmu, m, t, code, length);
		this.t2 = t2;
		this.actionTaken = false;
	}

	@Override
	void update(Clock clock) {
		clock.update(m, actionTaken ? t : t2);
	}
}
