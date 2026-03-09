package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

abstract class OperationADD_HL_rr extends Operation {

	private final Register register;

	OperationADD_HL_rr(Z80Cpu cpu, MMU mmu, int m, int t, int code, Register register) {
		super(cpu, mmu, m, t, code, 1);
		this.register = register;
	}

	@Override
	void execute() {
		int before = cpu.HL.getValue();
		int value = register.getValue();
		int result = before + value;
		cpu.HL.setValue(result & 0xFFFF);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, (value & 0xFFF) + (before & 0xFFF) > 0xFFF);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, result > 0xFFFF);

	}
}
