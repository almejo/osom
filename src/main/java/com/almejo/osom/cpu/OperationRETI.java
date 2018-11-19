package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRETI extends Operation {

	OperationRETI(Z80Cpu cpu, MMU mmu) {

		super(cpu, mmu, 1, 16, 0xd9, 1);
	}

	@Override
	void execute() {
		int oldValue = cpu.PC.getValue();
		cpu.PC.setValue(cpu.popWordOnStack());
		cpu.setInterruptionsEnabled(true);
		if (debug) {
			print("RETI ;" + Integer.toHexString(oldValue) + " -> " + Integer.toHexString(cpu.PC.getValue()));
		}
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "RETI";
	}
}
