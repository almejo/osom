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


	public int or(int a, int b) {
		int value = a | b;
		cpu.setFlag(Z80Cpu.FLAG_ZERO, value == 0);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		return value;
	}

	public void dec(Register register, boolean alterFlags) {
		int oldValue = register.getValue();
		register.setValue(register.getValue() - 1);
		if (alterFlags) {
			updateDecFlags(oldValue, 1);
		}
	}

	void decHI(Register register, boolean alterFlags) {
		int oldValue = register.getHi();
		register.setHi(oldValue == 0 ? 0xff : oldValue - 1);
		if (alterFlags) {
			updateDecFlags(oldValue, 1);
		}
	}

	void decLO(Register register, boolean alterFlags) {
		int oldValue = register.getLo();
		register.setLo(oldValue == 0 ? 0xff : oldValue - 1);
		if (alterFlags) {
			updateDecFlags(oldValue, 1);
		}
	}

	void incLO(Register register, boolean alterFlags) {
		int oldValue = register.getLo();
		register.setLo(0xff & oldValue + 1);
		if (alterFlags) {
			updateIncFlags(oldValue, 1);
		}
	}

	void incHI(Register register, boolean alterFlags) {
		int oldValue = register.getHi();
		register.setHi(0xff & oldValue + 1);
		if (alterFlags) {
			updateIncFlags(oldValue, 1);
		}
	}

	void cpHI(Register register, int n) {
		int value = register.getHi();
		updateDecFlags(value, n);
	}

	private void updateIncFlags(int value, int n) {
		cpu.setFlag(Z80Cpu.FLAG_ZERO, ((value + n) & 0xff) == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, (value & 0x0f) < (n & 0x0f));
	}

	public void addRegisterHI(Register register, int n) {
		int oldValue = register.getHi();
		register.setHi(register.getHi()+ n);
		updateAddFlags(oldValue, n);
	}

	private void updateAddFlags(int value, int n) {
		cpu.setFlag(Z80Cpu.FLAG_ZERO, ((value + n) & 0xff) == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, (value & 0xf) + (n & 0xf) > 0xf);
		cpu.setFlag(Z80Cpu.FLAG_CARRY, (value + n) > 0xff);
	}

	private void updateDecFlags(int value, int n) {
		cpu.setFlag(Z80Cpu.FLAG_ZERO, ((value - n) & 0xff) == 0);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, true);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, (value & 0x0f) < (n & 0x0f));
		cpu.setFlag(Z80Cpu.FLAG_CARRY, value < n);
	}

	public void setBIT(boolean equalsZero) {
		cpu.setFlag(Z80Cpu.FLAG_ZERO, equalsZero);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, true);
	}

	public int rotateLeft(int oldValue) {
		int value = (oldValue << 1) & 0xff;
		if (cpu.isFlagSetted(Z80Cpu.FLAG_CARRY)) {
			value = BitUtils.setBit(value, 0);
		}
		cpu.setFlag(Z80Cpu.FLAG_CARRY, BitUtils.isBitSetted(oldValue, 7));
		cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
		cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
		cpu.setFlag(Z80Cpu.FLAG_ZERO, value == 0);
		return value;
	}

	public void subHI(Register register, int value) {
		int before = register.getHi();
		register.setHi(register.getHi() - value);
		updateDecFlags(before, value);
	}
}