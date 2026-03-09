package com.almejo.osom.cpu;

public class BitUtils {
	static int setBit(int value, int n) {
		return value | 1 << n;
	}

	static int resetBit(int value, int n) {
		return value & ~(1 << n);
	}

	public static boolean isBitSetted(int value, int flag) {
		return (value & 1 << flag) > 0;
	}
}
