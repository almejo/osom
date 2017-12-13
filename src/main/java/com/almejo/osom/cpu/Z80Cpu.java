package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;
import lombok.Setter;

public class Z80Cpu {

	@Setter
	private MMU mmu;

	private Register AF = new Register();
	private Register BC = new Register();
	private Register DE = new Register();
	private Register HL = new Register();

	private static byte FLAG_Z = 7;
	private static byte FLAG_N = 6;
	private static byte FLAG_H = 5;
	private static byte FLAG_C = 4;

	private int programCounter;
	private Register stackPointer = new Register();


	public void reset() {
		programCounter = 0x100;
		stackPointer.setValue(0xFFFE);

		AF.setValue(0x01B0);
		BC.setValue(0x0013);
		DE.setValue(0x00D8);
		HL.setValue(0x014D);
		mmu.setByte(0xFF05, 0x00);
		mmu.setByte(0xFF06, 0x00);
		mmu.setByte(0xFF07, 0x00);
		mmu.setByte(0xFF10, 0x80);
		mmu.setByte(0xFF11, 0xBF);
		mmu.setByte(0xFF12, 0xF3);
		mmu.setByte(0xFF14, 0xBF);
		mmu.setByte(0xFF16, 0x3F);
		mmu.setByte(0xFF17, 0x00);
		mmu.setByte(0xFF19, 0xBF);
		mmu.setByte(0xFF1A, 0x7F);
		mmu.setByte(0xFF1B, 0xFF);
		mmu.setByte(0xFF1C, 0x9F);
		mmu.setByte(0xFF1E, 0xBF);
		mmu.setByte(0xFF20, 0xFF);
		mmu.setByte(0xFF21, 0x00);
		mmu.setByte(0xFF22, 0x00);
		mmu.setByte(0xFF23, 0xBF);
		mmu.setByte(0xFF24, 0x77);
		mmu.setByte(0xFF25, 0xF3);
		mmu.setByte(0xFF26, 0xF1);
		mmu.setByte(0xFF40, 0x91);
		mmu.setByte(0xFF42, 0x00);
		mmu.setByte(0xFF43, 0x00);
		mmu.setByte(0xFF45, 0x00);
		mmu.setByte(0xFF47, 0xFC);
		mmu.setByte(0xFF48, 0xFF);
		mmu.setByte(0xFF49, 0xFF);
		mmu.setByte(0xFF4A, 0x00);
		mmu.setByte(0xFF4B, 0x00);
		mmu.setByte(0xFFFF, 0x00);
	}

	public void execute(int cycles) {
		int operationCode = mmu.getByte(programCounter);
		programCounter++;
		System.out.println(Integer.toHexString(operationCode));
		switch (operationCode) {
			case 0x0:
				return;
			case 0xc3:
				executeC3();
				return;
			case 0xaf:
				executeAX(AF);
				return;
			default:
				throw new RuntimeException("code not found 0x" + Integer.toHexString(operationCode));
		}
	}

	private void executeAX(Register register) {
		register.setHi(register.getHi() ^ register.getLo());
	}

	private void executeC3() {
		int value = mmu.getByte(programCounter + 1) << 8 | mmu.getByte(programCounter);
		System.out.println("jp " + Integer.toOctalString(value));
		programCounter = value;
	}
}
