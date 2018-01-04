package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationRET_Z extends OperationConditional {

	OperationRET_Z(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 20, 8, 0xc8, 1);
	}

	@Override
	void execute() {
		int address = cpu.popWordOnStack();
		boolean jump = cpu.isFlagSetted(Z80Cpu.FLAG_ZERO);
		if (debug) {
			System.out.println("RET Z ;" +  jump + " " + cpu.PC);
		}
		if(jump) {
			cpu.PC.setValue(address);
		}
	}
}
