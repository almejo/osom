package com.almejo.osom.cpu;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class Register {
	private int lo;
	private int hi;

	void setValue(int value) {
		setLo(value & 0x00ff);
		setHi((value & 0xff00) >> 8);
	}

	int getValue() {
		return hi << 8 & lo;
	}
}
