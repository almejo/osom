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
			delta = -1 * (0xff - delta);
			// delta = -((~251+1)&0xff);
		}
		return delta;
	}

	void print(String string) {
		System.out.println(cpu.AF.debugStringHI() + " " + cpu.printFlags() + " " + cpu.BC.debugString() + " " + cpu.DE.debugString() + " " + cpu.HL.debugString() + " " + cpu.SP.debugString() + " " + cpu.PC.debugString() + " (cy: " + cpu.clock.getT()+ ") ppu:+0 |[00]0x"+cpu.PC.toHex() + ": " + getIntruction() + " " + string);
	}

	private String getIntruction() {
		StringBuilder string= new StringBuilder(BitUtils.toHex2(this.code));
		int len = 9 - string.length();
		for (int i = 0; i < len; i++) {
			string.append(" ");
		}
		return string.toString();
	}

	String hexAddr(int value) {
		return BitUtils.toHex(value).toUpperCase() + "H";
	}
}
