package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_HL_nn extends Operation {

	OperationLD_HL_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 12, 0x21, 3);
	}

	@Override
	void execute() {
		int value = mmu.getWord(cpu.PC.getValue() + 1);
		if (debug) {
			printWord("LD " + cpu.HL.getName() + "," + value, value);
		}
		cpu.HL.setValue(value);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LD " + cpu.HL.getName() + "," + toWord(ram[base + 2], ram[base + 1]);
	}
}
