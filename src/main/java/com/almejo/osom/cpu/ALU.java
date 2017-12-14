package com.almejo.osom.cpu;

public class ALU {
	private Z80Cpu cpu;

	ALU(Z80Cpu cpu) {
		this.cpu = cpu;
	}

	public int xor(int a, int b) {
		int value = a ^ b;
		cpu.setFlag(Z80Cpu.FLAG_ZERO, value == 0);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_SUBSTRACT, false);
		return value;
	}

	public void dec(Register register) {
		dec(register, true);
	}

	public void dec(Register register, boolean alterFlags) {
		register.setValue(register.getValue() - 1);
		if (alterFlags) {
			cpu.setFlag(Z80Cpu.FLAG_ZERO, register.getValue() == 0);
			cpu.setFlag(Z80Cpu.FLAG_SUBSTRACT, true);
		}
	}
}
