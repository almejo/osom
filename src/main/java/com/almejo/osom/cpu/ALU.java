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
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		return value;
	}

	public void dec(Register register, boolean alterFlags) {
		register.setValue(register.getValue() - 1);
		if (alterFlags) {
			cpu.setFlag(Z80Cpu.FLAG_ZERO, register.getValue() == 0);
			cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true);
			cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true);
			cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
		}
	}

	void decHI(Register register, boolean alterFlags) {
		int oldValue = register.getHi();
		register.setHi(oldValue == 0 ? 0xff : oldValue - 1);
		if (alterFlags) {
			updateDecFlags(oldValue);
		}
	}

	void cpHI(Register register, int n) {
		throw new RuntimeException("not implemented");
//			int oldValue = register.getHi();
//			cpu.setFlag(Z80Cpu.FLAG_ZERO, oldValue == 1);
//			cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true);
//			cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, oldValue == 0);
//			cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
	}

	private void updateDecFlags(int oldValue) {
		cpu.setFlag(Z80Cpu.FLAG_ZERO, oldValue == 1);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, oldValue == 0);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
	}

	void decLO(Register register, boolean alterFlags) {
		int oldValue = register.getLo();
		register.setLo(oldValue == 0 ? 0xff : oldValue - 1);
		if (alterFlags) {
			updateDecFlags(oldValue);
		}
	}
}
