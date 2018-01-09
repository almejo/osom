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

	public static String toHex(int value) {
		return String.format("%4s", Integer.toHexString(value)).replace(" ", "0");
	}

	public static String toHex2(int value) {
		return String.format("%2s", Integer.toHexString(value)).replace(" ", "0");
	}

}
