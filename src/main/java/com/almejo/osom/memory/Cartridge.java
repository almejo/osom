package com.almejo.osom.memory;

public class Cartridge {
	private int[] bytes;
	private String title;

	public Cartridge(byte[] bytes) {
		this.bytes = ByteUtils.getBytes(bytes);
		title = parseTitle();
		System.out.println("Title: " + title);
	}


	private String parseTitle() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 16; i++) {
			builder.append(this.bytes[0x134 + i] >= 0 ? "" + (char) (32 + this.bytes[0x134 + i]) : " ");
		}
		return builder.toString();
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
