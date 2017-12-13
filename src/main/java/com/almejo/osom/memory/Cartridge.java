package com.almejo.osom.memory;

public class Cartridge {
	private int[] bytes;

	public Cartridge(byte[] bytes) {
		this.bytes = new int[bytes.length];
		int index = 0;
		for (byte b : bytes) {
			this.bytes[index] = b < 0 ? 256 + b : b;
			index++;
		}
	}

	int getByte(int address) {
		return bytes[address];
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		int i = 1;
		for (int value : bytes) {
			if ((i - 1) % 32 == 0) {
				builder.append(i - 1);
				builder.append("|");
				builder.append(Integer.toHexString(i - 1));
				builder.append("-> ");
			}
			String string = Integer.toHexString(value);
			if (string.length() < 2) {
				string = "0" + string;
			}
			builder.append(string);
			if (i % 2 == 0) {
				builder.append(" ");
			}
			if (i % 32 == 0) {
				builder.append("\n");
			}
			i++;
		}
		return builder.toString();
	}
}
