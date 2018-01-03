package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_SP_nn extends Operation {

	private Register register;

	OperationLD_SP_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 12, 0x31, 3);
		this.register = cpu.SP;
	}

	@Override
	void execute() {
		int nn = mmu.getWord(cpu.PC.getValue() + 1);
		if (debug) {
			System.out.println("LD " + register.getName() + ", 0x" + Integer.toHexString(nn));
		}
		this.register.setValue(nn);
	}
}
