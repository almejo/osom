package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationXOR_n extends Operation {

	OperationXOR_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 2, 8, 0xee, 2);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		if (debug) {
			print("XOR d8");
		}
		cpu.AF.setHi(cpu.alu.xor(cpu.AF.getHi(), value));
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "XOR " +  BitUtils.toHex2(ram[base + 1])  + "h";
	}
}
