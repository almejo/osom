package com.almejo.osom.cpu;


import com.almejo.osom.memory.MMU;
import lombok.Getter;

public abstract class Operation {

	public static boolean debug = true;

	protected Z80Cpu cpu;

	protected MMU mmu;
	@Getter
	protected int code;

	@Getter
	private int length;

	@Getter
	protected int t;
	protected int m;

	Operation(Z80Cpu cpu, MMU mmu, int m, int t, int code, int length) {
		this.cpu = cpu;
		this.mmu = mmu;
		this.t = t;
		this.m = m;
		this.code = code;
		this.length = length;
	}

	void update(Clock clock) {
		clock.update(m, t);
	}

	abstract void execute();

	int toSignedByte(int val) {
		int delta = val;
		if (delta > 127) {
			delta = -1 * (0xff - delta + 1);
			// delta = -((~251+1)&0xff);
		}
		return delta;
	}

	void print(String string) {
		printStruction(string, getIntruction());
	}

	private void printStruction(String string, String instruction) {
		String output = cpu.AF.debugStringHI()
				+ " " + cpu.printFlags()
				+ " " + cpu.BC.debugString()
				+ " " + cpu.DE.debugString()
				+ " " + cpu.HL.debugString()
				+ " " + cpu.SP.debugString()
				+ " " + cpu.PC.debugString()
				+ " (cy: " + cpu.clock.getT() + ")"
				//+ " ppu:+" + cpu.getGpu().mode
				+ " ppu:+0"
				+ " |[00]0x" + cpu.PC.toHex() + ": " + instruction + " " + string;
		System.out.println(output.toLowerCase());
	}

	void printWord(String string, int value) {
		printStruction(string, getIntructionWord(value));
	}

	void printByte(String string, int value) {
		printStruction(string, getIntructionByte(value));
	}

	private String getIntruction() {
		return getStringSized(BitUtils.toHex2(this.code), 9);
	}

	private String getIntructionWord(int value) {
		return getStringSized(BitUtils.toHex2(this.code)
				+ " " + BitUtils.toHex2(value & 0xFF)
				+ " " + BitUtils.toHex2((value & 0xFF00) >> 8), 9);
	}

	private String getIntructionByte(int value) {
		return getStringSized(BitUtils.toHex2(this.code)
				+ " " + BitUtils.toHex2(value & 0xFF), 9);
	}

	private String getStringSized(String string, int size) {
		StringBuilder stringBuilder = new StringBuilder(string);
		int len = size - string.length();
		for (int i = 0; i < len; i++) {
			stringBuilder.append(" ");
		}
		return stringBuilder.toString();
	}

	String hexAddr(int value) {
		return BitUtils.toHex(value).toUpperCase() + "H";
	}
}
