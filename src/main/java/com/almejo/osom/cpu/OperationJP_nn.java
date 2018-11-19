package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationJP_nn extends Operation {

	OperationJP_nn(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 16, 0xc3, 3);
	}

	@Override
	void execute() {
		int value = mmu.getWord(cpu.PC.getValue() + 1);
		if (debug) {
			printWord("JP " + hexAddr(value), value);
		}
		cpu.PC.setValue(value);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "JP l" + toWord(ram[base + 2], ram[base + 1]);
	}
}
