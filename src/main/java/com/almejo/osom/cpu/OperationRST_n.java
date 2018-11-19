package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationRST_n extends Operation {

	private int n;

	OperationRST_n(Z80Cpu cpu, MMU mmu, int m, int t, int code, int n) {
		super(cpu, mmu, m, t, code, 1);
		this.n = n;
	}

	@Override
	void execute() {
		if (debug) {
			print("RST 0x" + Integer.toHexString(n));
		}
		cpu.pushWordOnStack(cpu.PC.getValue() + getLength());
		cpu.PC.setValue(n);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "RST " + Integer.toHexString(n) + "h";
	}
}
