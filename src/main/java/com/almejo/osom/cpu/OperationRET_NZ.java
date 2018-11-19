package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRET_NZ extends OperationConditional {

	OperationRET_NZ(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 20, 8, 0xc0, 1);
	}

	@Override
	void execute() {
		int address = cpu.popWordOnStack();
		boolean jump = !cpu.isFlagSetted(Z80Cpu.FLAG_ZERO);
		if (debug) {
			print("RET NZ ;" + jump + " " + cpu.PC);
		}
		if (jump) {
			cpu.PC.setValue(address);
		}
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "RET NZ";
	}
}
