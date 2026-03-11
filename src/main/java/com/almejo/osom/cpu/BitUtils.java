package com.almejo.osom.cpu;

public class BitUtils {
	public static int setBit(int value, int n) {
		return value | 1 << n;
	}

	public static int resetBit(int value, int n) {
		return value & ~(1 << n);
	}

	public static boolean isBitSetted(int value, int flag) {
		return (value & 1 << flag) > 0;
	}

	public static int toSignedByte(int value) {
		if (value > 127) {
			return value - 256;
		}
		return value;
	}
}
