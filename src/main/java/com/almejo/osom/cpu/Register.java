package com.almejo.osom.cpu;

import lombok.Getter;

class Register {
	@Getter
	private String name;

	@Getter
	private int lo;

	@Getter
	private int hi;

	Register(String name) {
		this.name = name;
	}

	void setValue(int value) {
		setHi((value & 0xff00) >> 8);
		setLo(value & 0x00ff);
	}

	public void setLo(int value) {
		this.lo = value & 0x00ff;
	}

	public void setHi(int value) {
		this.hi = value & 0x00ff;
	}

	int getValue() {
		return hi << 8 | lo;
	}

	@Override
	public String toString() {
		return this.name + "=0x" + Integer.toHexString(this.getValue()) + " (b" + Integer.toBinaryString(this.getHi()) + " " + Integer.toBinaryString(this.getLo()) + ")";
	}

	public void inc(int value) {
		setValue(getValue() + value);
	}

	public String getName(boolean lo) {
		return "" + (lo ? getName().charAt(1) : getName().charAt(0));
	}
}
