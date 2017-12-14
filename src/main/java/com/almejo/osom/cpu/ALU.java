package com.almejo.osom.cpu;

public class ALU {
	private Z80Cpu cpu;

	ALU(Z80Cpu cpu) {
		this.cpu = cpu;
	}

	public int xor(int a, int b) {
		int value = a ^ b;
		cpu.setFlag(Z80Cpu.FLAG_Z, value == 0);
		cpu.setFlag(Z80Cpu.FLAG_H, false);
		cpu.setFlag(Z80Cpu.FLAG_C, false);
		cpu.setFlag(Z80Cpu.FLAG_N, false);
		return value;
	}

	public void dec(Register register) {
		register.setValue(register.getValue() - 1);
	}
}
