package com.almejo.osom.cpu;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class Register {
	private String name;
	private int lo;
	private int hi;

	Register(String name) {
		this.name = name;
	}

	void setValue(int value) {
		setLo(value & 0x00ff);
		setHi((value & 0xff00) >> 8);
	}

	int getValue() {
		return hi << 8 | lo;
	}

	@Override
	public String toString() {
		return this.name+"="+ Integer.toHexString(this.getValue()) + "(" + Integer.toBinaryString(this.getValue()) + ")";
	}
}
