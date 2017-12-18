package com.almejo.osom.memory;

public class ByteUtils {
	public static int[] getBytes(byte[] bytes) {
		int []array = new int[bytes.length];
		int index = 0;
		for (byte b : bytes) {
			array[index] = b < 0 ? 256 + b : b;
			index++;
		}
		return array;
	}
}
