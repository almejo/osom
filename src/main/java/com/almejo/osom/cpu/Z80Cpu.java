package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Z80Cpu {
	private static final Map<Integer, Integer> INTERRUPT_ADDRESSES = new HashMap<>();
	public static final int INTERRUPT_ADDRESS_V_BLANK = 0x40;
	public static final int INTERRUPT_ADDRESS_LCD = 0x48;
	public static final int INTERRUPT_ADDRESS_TIMER = 0x50;
	public static final int INTERRUPT_ADDRESS_JOY_PAD = 0x60;

	private static final int INTERRUPT_BIT_V_BLANK = 0;
	private static final int INTERRUPT_BIT_LCD = 1;
	private static final int INTERRUPT_BIT_TIMER = 2;
	private static final int INTERRUPT_BIT_JOYPAD = 4;

	private static int timerCounter = 1024; // Default. 4096 hz
	private static int TIMER_ENABLED_BIT = 2;

	private boolean interruptionsEnabled = false;
	private static int PREFIX_CB = 0xcb;

	private HashMap<Integer, Operation> operations = new HashMap<>();

	private HashMap<Integer, Operation> operationsCB = new HashMap<>();

	static {
		INTERRUPT_ADDRESSES.put(INTERRUPT_BIT_V_BLANK, INTERRUPT_ADDRESS_V_BLANK);
		INTERRUPT_ADDRESSES.put(INTERRUPT_BIT_LCD, INTERRUPT_ADDRESS_LCD);
		INTERRUPT_ADDRESSES.put(INTERRUPT_BIT_TIMER, INTERRUPT_ADDRESS_TIMER);
		INTERRUPT_ADDRESSES.put(INTERRUPT_BIT_JOYPAD, INTERRUPT_ADDRESS_JOY_PAD);
	}

	@Setter
	private MMU mmu;
	private int cyclesPerSecond;

	final ALU alu;

	private List<Register> registers = new LinkedList<>();
	Register AF = new Register("AF");
	Register BC = new Register("BC");
	Register DE = new Register("DE");
	Register HL = new Register("HL");

	static byte FLAG_ZERO = 7;
	static byte FLAG_SUBTRACT = 6;
	static byte FLAG_HALF_CARRY = 5;
	static byte FLAG_CARRY = 4;

	Register PC = new Register("PC");
	Register SP = new Register("SP");
	public Clock clock = new Clock();
	private int dividerCounter;

	public Z80Cpu(MMU mmu, int cycles) {
		this.mmu = mmu;
		cyclesPerSecond = cycles;
		registers.add(AF);
		registers.add(BC);
		registers.add(DE);
		registers.add(HL);
		registers.add(PC);
		registers.add(SP);
		alu = new ALU(this);
		addOpcode(new OperationADD_aHL(this, this.mmu));
		addOpcode(new OperationNOOP(this, this.mmu));
		addOpcode(new OperationJP_nn(this, this.mmu));
		addOpcode(new OperationXOR_A(this, this.mmu));
		addOpcode(new OperationOR_C(this, this.mmu));
		addOpcode(new OperationLD_HL_nn(this, this.mmu));
		addOpcode(new OperationLD_BC_nn(this, this.mmu));

		addOpcode(new OperationLDD_HL_A(this, this.mmu));
		addOpcode(new OperationLD_SP_nn(this, this.mmu));
		addOpcode(new OperationLD_A_n(this, this.mmu));
		addOpcode(new OperationLD_B_n(this, this.mmu));
		addOpcode(new OperationLD_C_n(this, this.mmu));
		addOpcode(new OperationLD_D_n(this, this.mmu));
		addOpcode(new OperationLD_E_n(this, this.mmu));
		addOpcode(new OperationLD_L_n(this, this.mmu));
		addOpcode(new OperationLD_HL_n(this, this.mmu));

		addOpcode(new OperationDEC_B(this, this.mmu));
		addOpcode(new OperationDEC_BC(this, this.mmu));
		addOpcode(new OperationDEC_C(this, this.mmu));
		addOpcode(new OperationDEC_D(this, this.mmu));
		addOpcode(new OperationDEC_E(this, this.mmu));
		addOpcode(new OperationJR_NZ_n(this, this.mmu));
		addOpcode(new OperationJR_Z_n(this, this.mmu));
		addOpcode(new OperationJR_n(this, this.mmu));
		addOpcode(new OperationDI(this, this.mmu));
		addOpcode(new OperationLDH_n_A(this, this.mmu));
		addOpcode(new OperationLDH_A_n(this, this.mmu));
		addOpcode(new OperationLD_A_HLI(this, this.mmu));

		addOpcode(new OperationCP_HL(this, this.mmu));
		addOpcode(new OperationCP_n(this, this.mmu));
		addOpcode(new OperationBIT_7_H(this, this.mmu));
		addOpcode(new OperationLDH_C_A(this, this.mmu));
		addOpcode(new OperationINC_B(this, this.mmu));
		addOpcode(new OperationINC_C(this, this.mmu));
		addOpcode(new OperationINC_H(this, this.mmu));
		addOpcode(new OperationINC_HL(this, this.mmu));
		addOpcode(new OperationINC_DE(this, this.mmu));
		addOpcode(new OperationLD_HL_A(this, this.mmu));

		addOpcode(new OperationLD_DE_nn(this, this.mmu));
		addOpcode(new OperationLD_A_DE(this, this.mmu));
		addOpcode(new OperationCALL_nn(this, this.mmu));
		addOpcode(new OperationLD_C_A(this, this.mmu));
		addOpcode(new OperationLD_D_A(this, this.mmu));
		addOpcode(new OperationLD_H_A(this, this.mmu));
		addOpcode(new OperationPUSH_BC(this, this.mmu));
		addOpcode(new OperationRL_C(this, this.mmu));
		addOpcode(new OperationRLA(this, this.mmu));

		addOpcode(new OperationPOP_BC(this, this.mmu));
		addOpcode(new OperationLD_HLI_A(this, this.mmu));
		addOpcode(new OperationRET(this, this.mmu));
		addOpcode(new OperationLD_A_E(this, this.mmu));
		addOpcode(new OperationLD_A_H(this, this.mmu));
		addOpcode(new OperationLD_A_L(this, this.mmu));
		addOpcode(new OperationLD_A_B(this, this.mmu));
		addOpcode(new OperationLD_nn_A(this, this.mmu));
		addOpcode(new OperationDEC_A(this, this.mmu));
		addOpcode(new OperationSUB_B(this, this.mmu));

	}

	private void addOpcode(Operation operation) {
		if (operation instanceof OperationCB) {
			operationsCB.put(operation.code, operation);
			return;
		}
		operations.put(operation.code, operation);
	}

	public void reset(boolean bootBios) {
		PC.setValue(bootBios ? 0x0 : 0x100);
		SP.setValue(0xFFFE);
		AF.setValue(0x01B0);
		BC.setValue(0x0013);
		DE.setValue(0x00D8);
		HL.setValue(0x014D);
		resetMemory();
	}

	public void execute() {
		Operation operation;
		int operationCode = mmu.getByte(PC.getValue());
		if (operationCode == PREFIX_CB) {
			System.out.print("0xcb-");
			PC.inc(1);
			operationCode = mmu.getByte(PC.getValue());
			if (operationsCB.containsKey(operationCode)) {
				operation = operationsCB.get(operationCode);
			} else {
				throw new RuntimeException("code not found 0xcb 0x" + Integer.toHexString(operationCode));
			}
		} else if (operations.containsKey(operationCode)) {
			operation = operations.get(operationCode);
		} else {
			throw new RuntimeException("code not found 0x" + Integer.toHexString(operationCode));
		}
		System.out.print("0x" + Integer.toHexString(PC.getValue()) + " - ");
		System.out.print("0x" + Integer.toHexString(operationCode) + "] ");
		int oldPC = PC.getValue();
		operation.execute();
		operation.update(clock);
		if (PC.getValue() == oldPC) {
			PC.inc(operation.getLength());
		}
		printState();
	}

	private void printState() {
		StringBuilder builder = new StringBuilder();
		registers.forEach(register -> builder.append(register.toString()).append(" "));
		System.out.println(builder.append(" ").append(clock).toString());
	}


	void setFlag(byte flag, boolean set) {
		if (set) {
			AF.setLo(BitUtils.setBit(AF.getLo(), flag));
		} else {
			AF.setLo(BitUtils.resetBit(AF.getLo(), flag));
		}
	}

	boolean isFlagSetted(byte flag) {
		return BitUtils.isBitSetted(AF.getLo(), flag);
	}


	private void resetMemory() {
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

	public void setInterruptionsEnabled(boolean interruptionsEnabled) {
		this.interruptionsEnabled = interruptionsEnabled;
	}

	public void updateTimers(int cycles) {
		updateDividerRegister(cycles);
		updateTimerRegister(cycles);
	}

	private void updateDividerRegister(int cycles) {
		dividerCounter += cycles;
		if (dividerCounter >= 255) {
			dividerCounter = 0;
			this.mmu.incrementDividerRegister();
		}
	}

	private void updateTimerRegister(int cycles) {
		if (isClockEnabled()) {
			timerCounter -= cycles;
			if (timerCounter < 0) {
				if (timerIsAboutToOverflow()) {
					mmu.setByte(MMU.TIMER_ADDRESS, mmu.getByte(MMU.TIMER_MODULATOR));
					requestInterrupt(INTERRUPT_BIT_TIMER);
				} else {
					mmu.setByte(MMU.TIMER_ADDRESS, mmu.getByte(MMU.TIMER_ADDRESS) + 1);
				}
			}
		}
	}

	private boolean timerIsAboutToOverflow() {
		return mmu.getByte(MMU.TIMER_ADDRESS) == 255;
	}

	public void requestInterrupt(int bit) {
		int value = mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS);
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, BitUtils.setBit(value, bit));
	}

	private boolean isClockEnabled() {
//		return true;
		return BitUtils.isBitSetted(TIMER_ENABLED_BIT, mmu.getByte(MMU.TIMER_CONTROLLER));
	}

	public void updateTimerCounter(int value) {
		System.out.println("timer updated!!!");
		timerCounter = convertToTimerCycles(value);
	}

	private int convertToTimerCycles(int value) {
		switch (value) {
			case 0:
				return this.cyclesPerSecond / 4096;
			case 1:
				return this.cyclesPerSecond / 262144;
			case 2:
				return this.cyclesPerSecond / 65536;
			case 3:
				return this.cyclesPerSecond / 16384;
			default:
				return this.cyclesPerSecond / 4096;
		}
	}

	public void checkInterrupts() {
		if (interruptionsEnabled) {
			int requests = mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS);
			int enabledInterrupts = mmu.getByte(MMU.INTERRUPT_ENABLED_ADDRESS);
			for (int i = 0; i < 5; i++) {
				if (canServeInterrupt(requests, enabledInterrupts, i)) {
					serveInterrupt(i);
				}
			}
		}
	}

	private void serveInterrupt(int bit) {
		interruptionsEnabled = false;
		int value = mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS);
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, BitUtils.resetBit(value, bit));

		pushWordOnStack(PC.getValue());
		PC.setValue(INTERRUPT_ADDRESSES.get(bit));
	}

	public void pushWordOnStack(int value) {
//		int hi = value >> 8;
//		int lo = value & 0x00ff;
//		SP.dec(1);
//		mmu.setByte(SP.getValue(), hi);
//		SP.dec(1);
//		mmu.setByte(SP.getValue(), lo);
		SP.dec(2);
		mmu.setWord(SP.getValue(), value);
	}

	public int popWordOnStack() {
		int value = mmu.getWord(SP.getValue());
		SP.inc(2);
		return value;
	}

	private boolean canServeInterrupt(int requests, int enabledInterrupts, int i) {
		return BitUtils.isBitSetted(requests, i) && BitUtils.isBitSetted(enabledInterrupts, i);
	}

}
