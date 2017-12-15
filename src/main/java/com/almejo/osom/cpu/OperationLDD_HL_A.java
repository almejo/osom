package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLDD_HL_A extends Operation {

	OperationLDD_HL_A(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x32, 1);
	}

	@Override
	void execute() {
		int oldValue = mmu.getByte(cpu.HL.getValue());
		mmu.setByte(cpu.HL.getValue(), cpu.AF.getHi());
		int newValue = mmu.getByte(cpu.HL.getValue());

		System.out.println("LDD [" + cpu.HL.getName() + "], " + cpu.AF.getName().charAt(0) + "; " + Integer.toHexString(oldValue) + " ==> " + Integer.toHexString(newValue));
		cpu.alu.dec(cpu.HL, false);
	}
}
