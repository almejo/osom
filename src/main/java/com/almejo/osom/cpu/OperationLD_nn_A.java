package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_nn_A extends Operation {

	OperationLD_nn_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 16, 0xea, 3);
	}

	@Override
	void execute() {
		int value = cpu.AF.getHi();
		int address = mmu.getWord(cpu.PC.getValue() + 1);
		if (debug) {
			print("LD [0x" + Integer.toHexString(address) + "], A" + Integer.toHexString(value));
		}
		mmu.setByte(address, value);
	}


	@Override
	public String decoded(int[] ram, int base) {
		return "LD [0x" + toWord(ram[base + 2], ram[base + 1]) + "], A";
	}
}
