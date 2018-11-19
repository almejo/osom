package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_A_ann extends Operation {

	OperationLD_A_ann(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 3, 16, 0xfa, 3);
	}

	@Override
	void execute() {
		int address = mmu.getWord(cpu.PC.getValue() + 1);
		int value = mmu.getByte(address);
		cpu.AF.setHi(value);
		if (debug) {
			print("LD A, [0x" + Integer.toHexString(address) + "] ; 0x" + Integer.toHexString(value));
		}
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LD A, [0x" + toWord(ram[base + 2], ram[base + 1]) + "]";
	}
}
