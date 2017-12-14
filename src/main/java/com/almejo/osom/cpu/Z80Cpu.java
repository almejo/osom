package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

public class Z80Cpu {

	private static final int OPCODE_JP = 0xc3;
	private static final int OPCODE_XOR_A = 0xaf;
	private static final int OPCODE_LD_HL_NN = 0x21;
	private static final int OPCODE_LDD_HL_A = 0x32;
	private static final int OPCODE_LD_C_N = 0xe;
	private static final int OPCODE_LD_B_N = 0x6;

	@Setter
	private MMU mmu;

	private final ALU alu;

	private List<Register> registers = new LinkedList<>();
	private Register AF = new Register("AF");
	private Register BC = new Register("BC");
	private Register DE = new Register("DC");
	private Register HL = new Register("HL");

	static byte FLAG_Z = 7;
	static byte FLAG_N = 6;
	static byte FLAG_H = 5;
	static byte FLAG_C = 4;

	private int programCounter;
	private Register stackPointer = new Register("SP");

	public Z80Cpu() {
		registers.add(AF);
		registers.add(BC);
		registers.add(DE);
		registers.add(HL);
		alu = new ALU(this);
	}

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

	public void execute() {
		int operationCode = mmu.getByte(programCounter);
		programCounter++;
		switch (operationCode) {
			case 0x0:
				break;
			case OPCODE_JP:
				executeJP();
				break;
			case OPCODE_XOR_A:
				executeXOR_A(AF.getHi());
				break;
			case OPCODE_LD_HL_NN:
				executeLD_N_nn(HL);
				break;
			case OPCODE_LDD_HL_A:
				executeLDD_HL_A();
				break;
			case OPCODE_LD_C_N:
				executeLD_LO_n(BC);
				break;
			case OPCODE_LD_B_N:
				executeLD_HI_n(BC);
				break;
			default:
				throw new RuntimeException("code not found 0x" + Integer.toHexString(operationCode));
		}
		printRegisters();
	}

	private void executeLDD_HL_A() {
		mmu.setByte(HL.getValue(), AF.getHi());
		System.out.println("LDD [" + HL.getName() + "], " + AF.getName().charAt(0) + "; " + Integer.toHexString(mmu.getByte(HL.getValue())));
		alu.dec(HL);
	}

	private void executeLD_HI_n(Register register) {
		System.out.println("LD " + register.getName().charAt(0) + ", 0x" + Integer.toHexString(mmu.getByte(programCounter)));
		register.setHi(mmu.getByte(programCounter));
		programCounter++;
	}

	private void executeLD_LO_n(Register register) {
		System.out.println("LD " + register.getName().charAt(1) + ", 0x" + Integer.toHexString(mmu.getByte(programCounter)));
		register.setLo(mmu.getByte(programCounter));
		programCounter++;
	}

	private void printRegisters() {
		StringBuilder builder = new StringBuilder();
		registers.forEach(register -> builder.append(register.toString()).append(" "));
		System.out.println(builder
				.append("PC=").append(Integer.toHexString(programCounter))
				.append("(")
				.append(Integer.toHexString(programCounter))
				.append(")").toString());
	}

	private void executeLD_N_nn(Register register) {
		int value = mmu.getWord(programCounter);
		System.out.println("LD " + register.getName() + ", 0x" + Integer.toHexString(value));
		programCounter++;
		programCounter++;
	}

	private void executeXOR_A(int value) {
		System.out.println("XOR A");
		AF.setHi(alu.xor(AF.getHi(), value));
	}

	void setFlag(byte flag, boolean set) {
		if (set) {
			AF.setLo(AF.getLo() | 1 << flag);
		} else {
			AF.setLo(AF.getLo() & ~(1 << flag));
		}
	}

	private void executeJP() {
		int value = mmu.getWord(programCounter);
		System.out.println("jp " + Integer.toHexString(value));
		programCounter = value;
	}
}
