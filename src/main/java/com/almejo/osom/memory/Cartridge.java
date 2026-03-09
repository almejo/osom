package com.almejo.osom.memory;

public class Cartridge {
	private final int[] bytes;

	public Cartridge(byte[] bytes) {
		this.bytes = ByteUtils.getBytes(bytes);
	}

	int getByte(int address) {
		return bytes[address];
	}
}
