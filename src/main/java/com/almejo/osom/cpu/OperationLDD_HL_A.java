package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDD_HL_A extends Operation {

	OperationLDD_HL_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 8, 0x32, 1);
	}

	@Override
	void execute() {
		if (debug) {
			print("LDD (" + cpu.HL.getName() + ")," + cpu.AF.getName(false));
		}
		mmu.setByte(cpu.HL.getValue(), cpu.AF.getHi());
		cpu.alu.dec(cpu.HL, false);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "LDD (" + cpu.HL.getName() + ")," + cpu.AF.getName(false);
	}
}
