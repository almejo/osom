package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationAND_n extends Operation {

	OperationAND_n(Z80Cpu cpu, MMU mmu) {

		super(cpu, mmu, 2, 8, 0xe6, 2);
	}

	@Override
	void execute() {
		int value = mmu.getByte(cpu.PC.getValue() + 1);
		if (debug) {
			print("AND 0x" + Integer.toHexString(value));
		}
		int result = (cpu.AF.getHi() & value) & 0xff;
		cpu.AF.setHi(result);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "AND 0x" + Integer.toHexString(ram[base + 1]);
	}
}
