package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRLA extends Operation {

	OperationRLA(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu,1, 4, 0x17, 1);
	}


	@Override
	void execute() {
		int oldValue = cpu.AF.getHi();
		int value = cpu.alu.rotateLeft(oldValue);
		cpu.AF.setHi(value);
		System.out.println("RLA " + cpu.AF.getName(false) + "; //  0x" + Integer.toHexString(oldValue) + "--> 0x" + Integer.toHexString(value));
	}
}
