package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDAA extends Operation {

	OperationDAA(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x27, 1);
	}

	// Flags: Z=* N=- H=0 C=*
	@Override
	void execute() {
		int a = cpu.AF.getHi();
		if (!cpu.isFlagSetted(Z80Cpu.FLAG_SUBTRACT)) {
			if (cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY) || (a & 0x0F) > 9) {
				a += 0x06;
			}
			if (cpu.isFlagSetted(Z80Cpu.FLAG_CARRY) || a > 0x9F) {
				a += 0x60;
			}
		} else {
			if (cpu.isFlagSetted(Z80Cpu.FLAG_HALF_CARRY)) {
				a = (a - 0x06) & 0xFF;
			}
			if (cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)) {
				a -= 0x60;
			}
		}
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		if ((a & 0x100) == 0x100) {
			cpu.setFlag(Z80Cpu.FLAG_CARRY, true);
		}
		a &= 0xFF;
		cpu.setFlag(Z80Cpu.FLAG_ZERO, a == 0);
		cpu.AF.setHi(a);
	}
}
