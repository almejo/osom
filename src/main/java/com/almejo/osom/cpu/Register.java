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

	public int getValue() {
		return hi << 8 | lo;
	}

	@Override
	public String toString() {
		return this.name + "=0x" + toHex(this.getValue());// + "(b" + toBinary(this.getHi()) + " " + toBinary(this.getLo()) + ")";
	}


	public String debugString() {
		return this.name + ":" + toHex(this.getValue());
	}

	public String debugStringHI() {
		return this.getName(false) + ":" + toHex2(this.getHi());
	}

	private String toHex(int value) {
		return String.format("%4s", Integer.toHexString(value)).replace(" ", "0");
	}

	private String toHex2(int value) {
		return String.format("%2s", Integer.toHexString(value)).replace(" ", "0");
	}

	private String toBinary(int value) {
		return String.format("%8s", Integer.toBinaryString(value)).replace(" ", "0");
	}

	public void inc(int value) {
		setValue(getValue() + value);
	}

	public void dec(int value) {
		setValue(getValue() - value);
	}

	public String getName(boolean lo) {
		return "" + (lo ? getName().charAt(1) : getName().charAt(0));
	}

	public void set(boolean lo, int value) {
		if (lo) {
			setLo(value);
		} else {
			setHi(value);
		}
	}

	public String toHex() {
		return toHex(this.getValue());
	}
}
