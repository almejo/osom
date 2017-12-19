package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationJR_NZ_n extends Operation {

	OperationJR_NZ_n(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 0x20, 2);
	}

	@Override
	void execute() {
		int delta = mmu.getByte(cpu.PC.getValue() + 1);
		System.out.println("JR NZ," + Integer.toHexString(delta));
		if (!cpu.isFlagSetted(Z80Cpu.FLAG_ZERO)) {
			if (delta > 127) {
				delta = -1 * (0xff - delta);
				// delta = -((~251+1)&0xff);
			}
			cpu.PC.setValue(cpu.PC.getValue()  + 1 + delta);
		}
	}
}