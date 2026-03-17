package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public class Z80Cpu {
	public static final int INTERRUPT_ADDRESS_V_BLANK = 0x40;
	public static final int INTERRUPT_ADDRESS_LCD = 0x48;
	public static final int INTERRUPT_ADDRESS_TIMER = 0x50;
	public static final int INTERRUPT_ADDRESS_SERIAL = 0x58;
	public static final int INTERRUPT_ADDRESS_JOY_PAD = 0x60;

	private static final Map<Integer, Integer> INTERRUPT_ADDRESSES = Map.of(
			MMU.INTERRUPT_VBLANK, INTERRUPT_ADDRESS_V_BLANK,
			MMU.INTERRUPT_LCD_STAT, INTERRUPT_ADDRESS_LCD,
			MMU.INTERRUPT_TIMER, INTERRUPT_ADDRESS_TIMER,
			MMU.INTERRUPT_SERIAL, INTERRUPT_ADDRESS_SERIAL,
			MMU.INTERRUPT_JOYPAD, INTERRUPT_ADDRESS_JOY_PAD
	);

	private int timerCounter = 1024; // Default. 4096 hz
	private static final int TIMER_ENABLED_BIT = 2;

	@Setter
    private boolean interruptionsEnabled = false;
	private boolean pendingInterruptEnable = false;
	private static final int PREFIX_CB = 0xcb;

	private final HashMap<Integer, Operation> operations = new HashMap<>();
	private final HashMap<Integer, Operation> operationsCB = new HashMap<>();

	@Setter
	private MMU mmu;
	private final int cyclesPerSecond;

	@Setter
    private boolean traceEnabled;
	private final CpuTracer tracer = new CpuTracer();

	final ALU alu;

	private final List<Register> registers = new LinkedList<>();
	final Register AF = new Register("AF");
	final Register BC = new Register("BC");
	final Register DE = new Register("DE");
	final Register HL = new Register("HL");

	static final byte FLAG_ZERO = 7;
	static final byte FLAG_SUBTRACT = 6;
	static final byte FLAG_HALF_CARRY = 5;
	static final byte FLAG_CARRY = 4;

	public final Register PC = new Register("PC");
	final Register SP = new Register("SP");
	private final Clock clock = new Clock();
	@Getter
	@Setter
	private boolean halted;
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
		addOpcode(new OperationADD_A(this, this.mmu));
		addOpcode(new OperationADD_B(this, this.mmu));
		addOpcode(new OperationADD_C(this, this.mmu));
		addOpcode(new OperationADD_D(this, this.mmu));
		addOpcode(new OperationADD_E(this, this.mmu));
		addOpcode(new OperationADD_H(this, this.mmu));
		addOpcode(new OperationADD_L(this, this.mmu));
		addOpcode(new OperationAND_n(this, this.mmu));
		addOpcode(new OperationAND_A(this, this.mmu));
		addOpcode(new OperationAND_C(this, this.mmu));
		addOpcode(new OperationADD_aHL(this, this.mmu));
		addOpcode(new OperationADD_HL_BC(this, this.mmu));
		addOpcode(new OperationADD_HL_DE(this, this.mmu));
		addOpcode(new OperationADD_HL_HL(this, this.mmu));
		addOpcode(new OperationADD_HL_SP(this, this.mmu));
		addOpcode(new OperationNOOP(this, this.mmu));
		addOpcode(new OperationJP_nn(this, this.mmu));
		addOpcode(new OperationJP_Z_nn(this, this.mmu));
		addOpcode(new OperationJP_HL(this, this.mmu));
		addOpcode(new OperationXOR_A(this, this.mmu));
		addOpcode(new OperationXOR_C(this, this.mmu));
		addOpcode(new OperationOR_B(this, this.mmu));
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
		addOpcode(new OperationLD_A_ann(this, this.mmu));

		addOpcode(new OperationLD_A_aHL(this, this.mmu));
		addOpcode(new OperationLD_B_aHL(this, this.mmu));
		addOpcode(new OperationLD_C_aHL(this, this.mmu));
		addOpcode(new OperationLD_D_aHL(this, this.mmu));
		addOpcode(new OperationLD_E_aHL(this, this.mmu));
		addOpcode(new OperationLD_H_aHL(this, this.mmu));
		addOpcode(new OperationLD_L_aHL(this, this.mmu));


		addOpcode(new OperationDEC_B(this, this.mmu));
		addOpcode(new OperationDEC_BC(this, this.mmu));
		addOpcode(new OperationDEC_C(this, this.mmu));
		addOpcode(new OperationDEC_D(this, this.mmu));
		addOpcode(new OperationDEC_E(this, this.mmu));
		addOpcode(new OperationDEC_aHL(this, this.mmu));
		addOpcode(new OperationJR_NZ_n(this, this.mmu));
		addOpcode(new OperationJR_Z_n(this, this.mmu));
		addOpcode(new OperationJR_n(this, this.mmu));
		addOpcode(new OperationDI(this, this.mmu));
		addOpcode(new OperationEI(this, this.mmu));
		addOpcode(new OperationCPL(this, this.mmu));
		addOpcode(new OperationLDH_n_A(this, this.mmu));
		addOpcode(new OperationLDH_A_n(this, this.mmu));
		addOpcode(new OperationLD_A_HLI(this, this.mmu));

		addOpcode(new OperationCP_HL(this, this.mmu));
		addOpcode(new OperationCP_n(this, this.mmu));
		addOpcode(new OperationBIT_7_H(this, this.mmu));
		addOpcode(new OperationBIT_0_C(this, this.mmu));
		addOpcode(new OperationLDH_C_A(this, this.mmu));
		addOpcode(new OperationINC_A(this, this.mmu));
		addOpcode(new OperationINC_B(this, this.mmu));
		addOpcode(new OperationINC_C(this, this.mmu));
		addOpcode(new OperationINC_E(this, this.mmu));
		addOpcode(new OperationINC_H(this, this.mmu));
		addOpcode(new OperationINC_L(this, this.mmu));
		addOpcode(new OperationINC_BC(this, this.mmu));
		addOpcode(new OperationINC_DE(this, this.mmu));
		addOpcode(new OperationINC_HL(this, this.mmu));
		addOpcode(new OperationINC_aHL(this, this.mmu));
		addOpcode(new OperationLD_HL_A(this, this.mmu));
		addOpcode(new OperationLD_DE_A(this, this.mmu));


		addOpcode(new OperationLD_DE_nn(this, this.mmu));
		addOpcode(new OperationLD_A_BC(this, this.mmu));
		addOpcode(new OperationLD_A_DE(this, this.mmu));
		addOpcode(new OperationCALL_nn(this, this.mmu));
		addOpcode(new OperationLD_B_A(this, this.mmu));
		addOpcode(new OperationLD_C_A(this, this.mmu));
		addOpcode(new OperationLD_D_A(this, this.mmu));
		addOpcode(new OperationLD_E_A(this, this.mmu));

		addOpcode(new OperationLD_H_A(this, this.mmu));
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x6F, HL, true, AF, false));  // LD L, A
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x7F, AF, false, AF, false)); // LD A, A

		// LD r, r' (non-A register-to-register transfers)
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x40, BC, false, BC, false)); // LD B, B
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x41, BC, false, BC, true));  // LD B, C
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x42, BC, false, DE, false)); // LD B, D
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x43, BC, false, DE, true));  // LD B, E
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x44, BC, false, HL, false)); // LD B, H
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x45, BC, false, HL, true));  // LD B, L
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x48, BC, true, BC, false));  // LD C, B
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x49, BC, true, BC, true));   // LD C, C
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x4A, BC, true, DE, false));  // LD C, D
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x4B, BC, true, DE, true));   // LD C, E
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x4C, BC, true, HL, false));  // LD C, H
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x4D, BC, true, HL, true));   // LD C, L
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x50, DE, false, BC, false)); // LD D, B
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x51, DE, false, BC, true));  // LD D, C
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x52, DE, false, DE, false)); // LD D, D
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x53, DE, false, DE, true));  // LD D, E
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x54, DE, false, HL, false)); // LD D, H
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x55, DE, false, HL, true));  // LD D, L
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x58, DE, true, BC, false));  // LD E, B
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x59, DE, true, BC, true));   // LD E, C
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x5A, DE, true, DE, false));  // LD E, D
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x5B, DE, true, DE, true));   // LD E, E
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x5C, DE, true, HL, false));  // LD E, H
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x5D, DE, true, HL, true));   // LD E, L
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x60, HL, false, BC, false)); // LD H, B
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x61, HL, false, BC, true));  // LD H, C
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x62, HL, false, DE, false)); // LD H, D
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x63, HL, false, DE, true));  // LD H, E
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x64, HL, false, HL, false)); // LD H, H
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x65, HL, false, HL, true));  // LD H, L
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x68, HL, true, BC, false));  // LD L, B
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x69, HL, true, BC, true));   // LD L, C
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x6A, HL, true, DE, false));  // LD L, D
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x6B, HL, true, DE, true));   // LD L, E
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x6C, HL, true, HL, false));  // LD L, H
		addOpcode(new OperationLD_r_r(this, this.mmu, 0x6D, HL, true, HL, true));   // LD L, L

		addOpcode(new OperationPUSH_AF(this, this.mmu));
		addOpcode(new OperationPUSH_BC(this, this.mmu));
		addOpcode(new OperationPUSH_DE(this, this.mmu));
		addOpcode(new OperationPUSH_HL(this, this.mmu));
		addOpcode(new OperationRL_C(this, this.mmu));
		addOpcode(new OperationRLA(this, this.mmu));

		addOpcode(new OperationPOP_AF(this, this.mmu));
		addOpcode(new OperationPOP_BC(this, this.mmu));
		addOpcode(new OperationPOP_DE(this, this.mmu));
		addOpcode(new OperationPOP_HL(this, this.mmu));
		addOpcode(new OperationLD_HLI_A(this, this.mmu));
		addOpcode(new OperationRET(this, this.mmu));
		addOpcode(new OperationRETI(this, this.mmu));
		addOpcode(new OperationRET_Z(this, this.mmu));
		addOpcode(new OperationRET_NZ(this, this.mmu));
		addOpcode(new OperationLD_A_B(this, this.mmu));
		addOpcode(new OperationLD_A_C(this, this.mmu));
		addOpcode(new OperationLD_A_D(this, this.mmu));
		addOpcode(new OperationLD_A_E(this, this.mmu));
		addOpcode(new OperationLD_A_H(this, this.mmu));
		addOpcode(new OperationLD_A_L(this, this.mmu));
		addOpcode(new OperationLD_nn_A(this, this.mmu));
		addOpcode(new OperationDEC_A(this, this.mmu));
		addOpcode(new OperationSUB_B(this, this.mmu));
		addOpcode(new OperationSWAP_A(this, this.mmu));

		addOpcode(new OperationSLA_r(this, this.mmu, BC, false, 0x20)); // SLA B
		addOpcode(new OperationSLA_r(this, this.mmu, BC, true, 0x21));  // SLA C
		addOpcode(new OperationSLA_r(this, this.mmu, DE, false, 0x22)); // SLA D
		addOpcode(new OperationSLA_r(this, this.mmu, DE, true, 0x23));  // SLA E
		addOpcode(new OperationSLA_r(this, this.mmu, HL, false, 0x24)); // SLA H
		addOpcode(new OperationSLA_r(this, this.mmu, HL, true, 0x25));  // SLA L
		addOpcode(new OperationSLA_r(this, this.mmu, AF, false, 0x27)); // SLA A

		addOpcode(new OperationRST_28(this, this.mmu));
		addOpcode(new OperationRES_0_A(this, this.mmu));

// === REGULAR OPCODES ===
		addOpcode(new OperationLD_BC_A(this, this.mmu));
		addOpcode(new OperationRLCA(this, this.mmu));
		addOpcode(new OperationLD_nn_SP(this, this.mmu));
		addOpcode(new OperationRRCA(this, this.mmu));
		addOpcode(new OperationSTOP(this, this.mmu));
		addOpcode(new OperationINC_D(this, this.mmu));
		addOpcode(new OperationDEC_DE(this, this.mmu));
		addOpcode(new OperationRRA(this, this.mmu));
		addOpcode(new OperationDEC_H(this, this.mmu));
		addOpcode(new OperationLD_H_n(this, this.mmu));
		addOpcode(new OperationDAA(this, this.mmu));
		addOpcode(new OperationDEC_HL(this, this.mmu));
		addOpcode(new OperationDEC_L(this, this.mmu));
		addOpcode(new OperationJR_NC_n(this, this.mmu));
		addOpcode(new OperationINC_SP(this, this.mmu));
		addOpcode(new OperationSCF(this, this.mmu));
		addOpcode(new OperationJR_C_n(this, this.mmu));
		addOpcode(new OperationLD_A_HLD(this, this.mmu));
		addOpcode(new OperationDEC_SP(this, this.mmu));
		addOpcode(new OperationCCF(this, this.mmu));
		addOpcode(new OperationHALT(this, this.mmu));
		addOpcode(new OperationJP_NZ_nn(this, this.mmu));
		addOpcode(new OperationJP_NC_nn(this, this.mmu));
		addOpcode(new OperationJP_C_nn(this, this.mmu));
		addOpcode(new OperationCALL_NZ_nn(this, this.mmu));
		addOpcode(new OperationCALL_Z_nn(this, this.mmu));
		addOpcode(new OperationCALL_NC_nn(this, this.mmu));
		addOpcode(new OperationCALL_C_nn(this, this.mmu));
		addOpcode(new OperationRET_NC(this, this.mmu));
		addOpcode(new OperationRET_C(this, this.mmu));
		addOpcode(new OperationADD_A_n(this, this.mmu));
		addOpcode(new OperationADC_A_n(this, this.mmu));
		addOpcode(new OperationSUB_n(this, this.mmu));
		addOpcode(new OperationSBC_A_n(this, this.mmu));
		addOpcode(new OperationXOR_n(this, this.mmu));
		addOpcode(new OperationOR_n(this, this.mmu));
		addOpcode(new OperationADD_SP_n(this, this.mmu));
		addOpcode(new OperationLDH_A_C(this, this.mmu));
		addOpcode(new OperationLD_HL_SP_n(this, this.mmu));
		addOpcode(new OperationLD_SP_HL(this, this.mmu));
		addOpcode(new OperationRST_00(this, this.mmu));
		addOpcode(new OperationRST_08(this, this.mmu));
		addOpcode(new OperationRST_10(this, this.mmu));
		addOpcode(new OperationRST_18(this, this.mmu));
		addOpcode(new OperationRST_20(this, this.mmu));
		addOpcode(new OperationRST_30(this, this.mmu));
		addOpcode(new OperationRST_38(this, this.mmu));
		addOpcode(new OperationLD_aHL_r(this, this.mmu, 0x70, BC, false)); // LD (HL), B
		addOpcode(new OperationLD_aHL_r(this, this.mmu, 0x71, BC, true)); // LD (HL), C
		addOpcode(new OperationLD_aHL_r(this, this.mmu, 0x72, DE, false)); // LD (HL), D
		addOpcode(new OperationLD_aHL_r(this, this.mmu, 0x73, DE, true)); // LD (HL), E
		addOpcode(new OperationLD_aHL_r(this, this.mmu, 0x74, HL, false)); // LD (HL), H
		addOpcode(new OperationLD_aHL_r(this, this.mmu, 0x75, HL, true)); // LD (HL), L
		addOpcode(new OperationADC_r(this, this.mmu, 0x88, BC, false)); // ADC A, B
		addOpcode(new OperationADC_r(this, this.mmu, 0x89, BC, true)); // ADC A, C
		addOpcode(new OperationADC_r(this, this.mmu, 0x8A, DE, false)); // ADC A, D
		addOpcode(new OperationADC_r(this, this.mmu, 0x8B, DE, true)); // ADC A, E
		addOpcode(new OperationADC_r(this, this.mmu, 0x8C, HL, false)); // ADC A, H
		addOpcode(new OperationADC_r(this, this.mmu, 0x8D, HL, true)); // ADC A, L
		addOpcode(new OperationADC_r(this, this.mmu, 0x8F, AF, false)); // ADC A, A
		addOpcode(new OperationADC_aHL(this, this.mmu)); // ADC A, (HL)
		addOpcode(new OperationSUB_r(this, this.mmu,0x91, BC, true)); // SUB C
		addOpcode(new OperationSUB_r(this, this.mmu,0x92, DE, false)); // SUB D
		addOpcode(new OperationSUB_r(this, this.mmu,0x93, DE, true)); // SUB E
		addOpcode(new OperationSUB_r(this, this.mmu,0x94, HL, false)); // SUB H
		addOpcode(new OperationSUB_r(this, this.mmu,0x95, HL, true)); // SUB L
		addOpcode(new OperationSUB_r(this, this.mmu,0x97, AF, false)); // SUB A
		addOpcode(new OperationSUB_aHL(this, this.mmu)); // SUB (HL)
		addOpcode(new OperationSBC_r(this, this.mmu, 0x98, BC, false)); // SBC A, B
		addOpcode(new OperationSBC_r(this, this.mmu, 0x99, BC, true)); // SBC A, C
		addOpcode(new OperationSBC_r(this, this.mmu, 0x9A, DE, false)); // SBC A, D
		addOpcode(new OperationSBC_r(this, this.mmu, 0x9B, DE, true)); // SBC A, E
		addOpcode(new OperationSBC_r(this, this.mmu, 0x9C, HL, false)); // SBC A, H
		addOpcode(new OperationSBC_r(this, this.mmu, 0x9D, HL, true)); // SBC A, L
		addOpcode(new OperationSBC_r(this, this.mmu, 0x9F, AF, false)); // SBC A, A
		addOpcode(new OperationSBC_aHL(this, this.mmu)); // SBC A, (HL)
		addOpcode(new OperationAND_r(this, this.mmu,0xA0, BC, false)); // AND B
		addOpcode(new OperationAND_r(this, this.mmu,0xA2, DE, false)); // AND D
		addOpcode(new OperationAND_r(this, this.mmu,0xA3, DE, true)); // AND E
		addOpcode(new OperationAND_r(this, this.mmu,0xA4, HL, false)); // AND H
		addOpcode(new OperationAND_r(this, this.mmu,0xA5, HL, true)); // AND L
		addOpcode(new OperationAND_aHL(this, this.mmu)); // AND (HL)
		addOpcode(new OperationXOR_r(this, this.mmu, 0xA8, BC, false)); // XOR B
		addOpcode(new OperationXOR_r(this, this.mmu, 0xAA, DE, false)); // XOR D
		addOpcode(new OperationXOR_r(this, this.mmu, 0xAB, DE, true)); // XOR E
		addOpcode(new OperationXOR_r(this, this.mmu, 0xAC, HL, false)); // XOR H
		addOpcode(new OperationXOR_r(this, this.mmu, 0xAD, HL, true)); // XOR L
		addOpcode(new OperationXOR_aHL(this, this.mmu)); // XOR (HL)
		addOpcode(new OperationOR_r(this, this.mmu,0xB2, DE, false)); // OR D
		addOpcode(new OperationOR_r(this, this.mmu,0xB3, DE, true)); // OR E
		addOpcode(new OperationOR_r(this, this.mmu,0xB4, HL, false)); // OR H
		addOpcode(new OperationOR_r(this, this.mmu,0xB5, HL, true)); // OR L
		addOpcode(new OperationOR_r(this, this.mmu,0xB7, AF, false)); // OR A
		addOpcode(new OperationOR_aHL(this, this.mmu)); // OR (HL)
		addOpcode(new OperationCP_r(this, this.mmu, 0xB8, BC, false)); // CP B
		addOpcode(new OperationCP_r(this, this.mmu, 0xB9, BC, true)); // CP C
		addOpcode(new OperationCP_r(this, this.mmu, 0xBA, DE, false)); // CP D
		addOpcode(new OperationCP_r(this, this.mmu, 0xBB, DE, true)); // CP E
		addOpcode(new OperationCP_r(this, this.mmu, 0xBC, HL, false)); // CP H
		addOpcode(new OperationCP_r(this, this.mmu, 0xBD, HL, true)); // CP L
		addOpcode(new OperationCP_r(this, this.mmu, 0xBF, AF, false)); // CP A

// === CB OPCODES ===
		addOpcode(new OperationRLC_r(this, this.mmu, 0x00, BC, false)); // RLC B
		addOpcode(new OperationRLC_r(this, this.mmu, 0x01, BC, true)); // RLC C
		addOpcode(new OperationRLC_r(this, this.mmu, 0x02, DE, false)); // RLC D
		addOpcode(new OperationRLC_r(this, this.mmu, 0x03, DE, true)); // RLC E
		addOpcode(new OperationRLC_r(this, this.mmu, 0x04, HL, false)); // RLC H
		addOpcode(new OperationRLC_r(this, this.mmu, 0x05, HL, true)); // RLC L
		addOpcode(new OperationRLC_r(this, this.mmu, 0x07, AF, false)); // RLC A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x06, OperationCB_aHL.OP_RLC, 0)); // RLC (HL)
		addOpcode(new OperationRRC_r(this, this.mmu, 0x08, BC, false)); // RRC B
		addOpcode(new OperationRRC_r(this, this.mmu, 0x09, BC, true)); // RRC C
		addOpcode(new OperationRRC_r(this, this.mmu, 0x0A, DE, false)); // RRC D
		addOpcode(new OperationRRC_r(this, this.mmu, 0x0B, DE, true)); // RRC E
		addOpcode(new OperationRRC_r(this, this.mmu, 0x0C, HL, false)); // RRC H
		addOpcode(new OperationRRC_r(this, this.mmu, 0x0D, HL, true)); // RRC L
		addOpcode(new OperationRRC_r(this, this.mmu, 0x0F, AF, false)); // RRC A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x0E, OperationCB_aHL.OP_RRC, 0)); // RRC (HL)
		addOpcode(new OperationRL_r(this, this.mmu, BC, false, 2, 8, 0x10)); // RL B
		addOpcode(new OperationRL_r(this, this.mmu, DE, false, 2, 8, 0x12)); // RL D
		addOpcode(new OperationRL_r(this, this.mmu, DE, true, 2, 8, 0x13)); // RL E
		addOpcode(new OperationRL_r(this, this.mmu, HL, false, 2, 8, 0x14)); // RL H
		addOpcode(new OperationRL_r(this, this.mmu, HL, true, 2, 8, 0x15)); // RL L
		addOpcode(new OperationRL_r(this, this.mmu, AF, false, 2, 8, 0x17)); // RL A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x16, OperationCB_aHL.OP_RL, 0)); // RL (HL)
		addOpcode(new OperationRR_r(this, this.mmu, 0x18, BC, false)); // RR B
		addOpcode(new OperationRR_r(this, this.mmu, 0x19, BC, true)); // RR C
		addOpcode(new OperationRR_r(this, this.mmu, 0x1A, DE, false)); // RR D
		addOpcode(new OperationRR_r(this, this.mmu, 0x1B, DE, true)); // RR E
		addOpcode(new OperationRR_r(this, this.mmu, 0x1C, HL, false)); // RR H
		addOpcode(new OperationRR_r(this, this.mmu, 0x1D, HL, true)); // RR L
		addOpcode(new OperationRR_r(this, this.mmu, 0x1F, AF, false)); // RR A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x1E, OperationCB_aHL.OP_RR, 0)); // RR (HL)
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x26, OperationCB_aHL.OP_SLA, 0)); // SLA (HL)
		addOpcode(new OperationSRA_r(this, this.mmu, 0x28, BC, false)); // SRA B
		addOpcode(new OperationSRA_r(this, this.mmu, 0x29, BC, true)); // SRA C
		addOpcode(new OperationSRA_r(this, this.mmu, 0x2A, DE, false)); // SRA D
		addOpcode(new OperationSRA_r(this, this.mmu, 0x2B, DE, true)); // SRA E
		addOpcode(new OperationSRA_r(this, this.mmu, 0x2C, HL, false)); // SRA H
		addOpcode(new OperationSRA_r(this, this.mmu, 0x2D, HL, true)); // SRA L
		addOpcode(new OperationSRA_r(this, this.mmu, 0x2F, AF, false)); // SRA A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x2E, OperationCB_aHL.OP_SRA, 0)); // SRA (HL)
		addOpcode(new OperationSWAP_r(this, this.mmu, 2, 8, 0x30, 1, BC, false)); // SWAP B
		addOpcode(new OperationSWAP_r(this, this.mmu, 2, 8, 0x31, 1, BC, true)); // SWAP C
		addOpcode(new OperationSWAP_r(this, this.mmu, 2, 8, 0x32, 1, DE, false)); // SWAP D
		addOpcode(new OperationSWAP_r(this, this.mmu, 2, 8, 0x33, 1, DE, true)); // SWAP E
		addOpcode(new OperationSWAP_r(this, this.mmu, 2, 8, 0x34, 1, HL, false)); // SWAP H
		addOpcode(new OperationSWAP_r(this, this.mmu, 2, 8, 0x35, 1, HL, true)); // SWAP L
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x36, OperationCB_aHL.OP_SWAP, 0)); // SWAP (HL)
		addOpcode(new OperationSRL_r(this, this.mmu, 0x38, BC, false)); // SRL B
		addOpcode(new OperationSRL_r(this, this.mmu, 0x39, BC, true)); // SRL C
		addOpcode(new OperationSRL_r(this, this.mmu, 0x3A, DE, false)); // SRL D
		addOpcode(new OperationSRL_r(this, this.mmu, 0x3B, DE, true)); // SRL E
		addOpcode(new OperationSRL_r(this, this.mmu, 0x3C, HL, false)); // SRL H
		addOpcode(new OperationSRL_r(this, this.mmu, 0x3D, HL, true)); // SRL L
		addOpcode(new OperationSRL_r(this, this.mmu, 0x3F, AF, false)); // SRL A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x3E, OperationCB_aHL.OP_SRL, 0)); // SRL (HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x40, 1, BC, 0, false)); // BIT 0,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x42, 1, DE, 0, false)); // BIT 0,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x43, 1, DE, 0, true)); // BIT 0,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x44, 1, HL, 0, false)); // BIT 0,H
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x45, 1, HL, 0, true)); // BIT 0,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x47, 1, AF, 0, false)); // BIT 0,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x46, OperationCB_aHL.OP_BIT, 0)); // BIT 0,(HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x48, 1, BC, 1, false)); // BIT 1,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x49, 1, BC, 1, true)); // BIT 1,C
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x4A, 1, DE, 1, false)); // BIT 1,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x4B, 1, DE, 1, true)); // BIT 1,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x4C, 1, HL, 1, false)); // BIT 1,H
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x4D, 1, HL, 1, true)); // BIT 1,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x4F, 1, AF, 1, false)); // BIT 1,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x4E, OperationCB_aHL.OP_BIT, 1)); // BIT 1,(HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x50, 1, BC, 2, false)); // BIT 2,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x51, 1, BC, 2, true)); // BIT 2,C
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x52, 1, DE, 2, false)); // BIT 2,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x53, 1, DE, 2, true)); // BIT 2,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x54, 1, HL, 2, false)); // BIT 2,H
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x55, 1, HL, 2, true)); // BIT 2,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x57, 1, AF, 2, false)); // BIT 2,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x56, OperationCB_aHL.OP_BIT, 2)); // BIT 2,(HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x58, 1, BC, 3, false)); // BIT 3,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x59, 1, BC, 3, true)); // BIT 3,C
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x5A, 1, DE, 3, false)); // BIT 3,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x5B, 1, DE, 3, true)); // BIT 3,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x5C, 1, HL, 3, false)); // BIT 3,H
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x5D, 1, HL, 3, true)); // BIT 3,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x5F, 1, AF, 3, false)); // BIT 3,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x5E, OperationCB_aHL.OP_BIT, 3)); // BIT 3,(HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x60, 1, BC, 4, false)); // BIT 4,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x61, 1, BC, 4, true)); // BIT 4,C
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x62, 1, DE, 4, false)); // BIT 4,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x63, 1, DE, 4, true)); // BIT 4,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x64, 1, HL, 4, false)); // BIT 4,H
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x65, 1, HL, 4, true)); // BIT 4,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x67, 1, AF, 4, false)); // BIT 4,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x66, OperationCB_aHL.OP_BIT, 4)); // BIT 4,(HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x68, 1, BC, 5, false)); // BIT 5,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x69, 1, BC, 5, true)); // BIT 5,C
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x6A, 1, DE, 5, false)); // BIT 5,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x6B, 1, DE, 5, true)); // BIT 5,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x6C, 1, HL, 5, false)); // BIT 5,H
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x6D, 1, HL, 5, true)); // BIT 5,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x6F, 1, AF, 5, false)); // BIT 5,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x6E, OperationCB_aHL.OP_BIT, 5)); // BIT 5,(HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x70, 1, BC, 6, false)); // BIT 6,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x71, 1, BC, 6, true)); // BIT 6,C
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x72, 1, DE, 6, false)); // BIT 6,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x73, 1, DE, 6, true)); // BIT 6,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x74, 1, HL, 6, false)); // BIT 6,H
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x75, 1, HL, 6, true)); // BIT 6,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x77, 1, AF, 6, false)); // BIT 6,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x76, OperationCB_aHL.OP_BIT, 6)); // BIT 6,(HL)
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x78, 1, BC, 7, false)); // BIT 7,B
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x79, 1, BC, 7, true)); // BIT 7,C
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x7A, 1, DE, 7, false)); // BIT 7,D
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x7B, 1, DE, 7, true)); // BIT 7,E
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x7D, 1, HL, 7, true)); // BIT 7,L
		addOpcode(new OperationBIT_b_r(this, this.mmu, 2, 8, 0x7F, 1, AF, 7, false)); // BIT 7,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x7E, OperationCB_aHL.OP_BIT, 7)); // BIT 7,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x80, BC, false, 0)); // RES 0,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x81, BC, true, 0)); // RES 0,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x82, DE, false, 0)); // RES 0,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x83, DE, true, 0)); // RES 0,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x84, HL, false, 0)); // RES 0,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x85, HL, true, 0)); // RES 0,L
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x86, OperationCB_aHL.OP_RES, 0)); // RES 0,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x88, BC, false, 1)); // RES 1,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x89, BC, true, 1)); // RES 1,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x8A, DE, false, 1)); // RES 1,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x8B, DE, true, 1)); // RES 1,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x8C, HL, false, 1)); // RES 1,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x8D, HL, true, 1)); // RES 1,L
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x8F, AF, false, 1)); // RES 1,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x8E, OperationCB_aHL.OP_RES, 1)); // RES 1,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x90, BC, false, 2)); // RES 2,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x91, BC, true, 2)); // RES 2,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x92, DE, false, 2)); // RES 2,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x93, DE, true, 2)); // RES 2,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x94, HL, false, 2)); // RES 2,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x95, HL, true, 2)); // RES 2,L
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x97, AF, false, 2)); // RES 2,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x96, OperationCB_aHL.OP_RES, 2)); // RES 2,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x98, BC, false, 3)); // RES 3,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x99, BC, true, 3)); // RES 3,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x9A, DE, false, 3)); // RES 3,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x9B, DE, true, 3)); // RES 3,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x9C, HL, false, 3)); // RES 3,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x9D, HL, true, 3)); // RES 3,L
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0x9F, AF, false, 3)); // RES 3,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0x9E, OperationCB_aHL.OP_RES, 3)); // RES 3,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA0, BC, false, 4)); // RES 4,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA1, BC, true, 4)); // RES 4,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA2, DE, false, 4)); // RES 4,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA3, DE, true, 4)); // RES 4,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA4, HL, false, 4)); // RES 4,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA5, HL, true, 4)); // RES 4,L
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA7, AF, false, 4)); // RES 4,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xA6, OperationCB_aHL.OP_RES, 4)); // RES 4,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA8, BC, false, 5)); // RES 5,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xA9, BC, true, 5)); // RES 5,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xAA, DE, false, 5)); // RES 5,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xAB, DE, true, 5)); // RES 5,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xAC, HL, false, 5)); // RES 5,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xAD, HL, true, 5)); // RES 5,L
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xAF, AF, false, 5)); // RES 5,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xAE, OperationCB_aHL.OP_RES, 5)); // RES 5,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB0, BC, false, 6)); // RES 6,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB1, BC, true, 6)); // RES 6,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB2, DE, false, 6)); // RES 6,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB3, DE, true, 6)); // RES 6,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB4, HL, false, 6)); // RES 6,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB5, HL, true, 6)); // RES 6,L
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB7, AF, false, 6)); // RES 6,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xB6, OperationCB_aHL.OP_RES, 6)); // RES 6,(HL)
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB8, BC, false, 7)); // RES 7,B
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xB9, BC, true, 7)); // RES 7,C
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xBA, DE, false, 7)); // RES 7,D
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xBB, DE, true, 7)); // RES 7,E
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xBC, HL, false, 7)); // RES 7,H
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xBD, HL, true, 7)); // RES 7,L
		addOpcode(new OperationRES_n_r(this, this.mmu, 2, 8, 0xBF, AF, false, 7)); // RES 7,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xBE, OperationCB_aHL.OP_RES, 7)); // RES 7,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC0, BC, false, 0)); // SET 0,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC1, BC, true, 0)); // SET 0,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC2, DE, false, 0)); // SET 0,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC3, DE, true, 0)); // SET 0,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC4, HL, false, 0)); // SET 0,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC5, HL, true, 0)); // SET 0,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC7, AF, false, 0)); // SET 0,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xC6, OperationCB_aHL.OP_SET, 0)); // SET 0,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC8, BC, false, 1)); // SET 1,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xC9, BC, true, 1)); // SET 1,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xCA, DE, false, 1)); // SET 1,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xCB, DE, true, 1)); // SET 1,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xCC, HL, false, 1)); // SET 1,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xCD, HL, true, 1)); // SET 1,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xCF, AF, false, 1)); // SET 1,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xCE, OperationCB_aHL.OP_SET, 1)); // SET 1,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD0, BC, false, 2)); // SET 2,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD1, BC, true, 2)); // SET 2,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD2, DE, false, 2)); // SET 2,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD3, DE, true, 2)); // SET 2,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD4, HL, false, 2)); // SET 2,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD5, HL, true, 2)); // SET 2,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD7, AF, false, 2)); // SET 2,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xD6, OperationCB_aHL.OP_SET, 2)); // SET 2,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD8, BC, false, 3)); // SET 3,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xD9, BC, true, 3)); // SET 3,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xDA, DE, false, 3)); // SET 3,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xDB, DE, true, 3)); // SET 3,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xDC, HL, false, 3)); // SET 3,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xDD, HL, true, 3)); // SET 3,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xDF, AF, false, 3)); // SET 3,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xDE, OperationCB_aHL.OP_SET, 3)); // SET 3,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE0, BC, false, 4)); // SET 4,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE1, BC, true, 4)); // SET 4,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE2, DE, false, 4)); // SET 4,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE3, DE, true, 4)); // SET 4,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE4, HL, false, 4)); // SET 4,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE5, HL, true, 4)); // SET 4,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE7, AF, false, 4)); // SET 4,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xE6, OperationCB_aHL.OP_SET, 4)); // SET 4,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE8, BC, false, 5)); // SET 5,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xE9, BC, true, 5)); // SET 5,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xEA, DE, false, 5)); // SET 5,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xEB, DE, true, 5)); // SET 5,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xEC, HL, false, 5)); // SET 5,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xED, HL, true, 5)); // SET 5,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xEF, AF, false, 5)); // SET 5,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xEE, OperationCB_aHL.OP_SET, 5)); // SET 5,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF0, BC, false, 6)); // SET 6,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF1, BC, true, 6)); // SET 6,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF2, DE, false, 6)); // SET 6,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF3, DE, true, 6)); // SET 6,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF4, HL, false, 6)); // SET 6,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF5, HL, true, 6)); // SET 6,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF7, AF, false, 6)); // SET 6,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xF6, OperationCB_aHL.OP_SET, 6)); // SET 6,(HL)
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF8, BC, false, 7)); // SET 7,B
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xF9, BC, true, 7)); // SET 7,C
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xFA, DE, false, 7)); // SET 7,D
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xFB, DE, true, 7)); // SET 7,E
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xFC, HL, false, 7)); // SET 7,H
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xFD, HL, true, 7)); // SET 7,L
		addOpcode(new OperationSET_b_r(this, this.mmu, 0xFF, AF, false, 7)); // SET 7,A
		addOpcode(new OperationCB_aHL(this, this.mmu, 0xFE, OperationCB_aHL.OP_SET, 7)); // SET 7,(HL)

	}

	private void addOpcode(Operation operation) {
		if (operation instanceof OperationCB) {
			if (operationsCB.containsKey(operation.code)) {
				throw new IllegalStateException(String.format(
						"CB opcode collision at 0x%02X: %s vs %s",
						operation.code,
						operationsCB.get(operation.code).getClass().getSimpleName(),
						operation.getClass().getSimpleName()));
			}
			operationsCB.put(operation.code, operation);
			return;
		}
		if (operations.containsKey(operation.code)) {
			throw new IllegalStateException(String.format(
					"Opcode collision at 0x%02X: %s vs %s",
					operation.code,
					operations.get(operation.code).getClass().getSimpleName(),
					operation.getClass().getSimpleName()));
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

    public int getProgramCounter() {
		return PC.getValue();
	}

	public int getClockT() {
		return clock.getT();
	}

	public void execute() {
		if (halted) {
			clock.update(1, 4);
			return;
		}

		if (traceEnabled) {
			tracer.traceLine(this, mmu);
		}

		if (pendingInterruptEnable) {
			interruptionsEnabled = true;
			pendingInterruptEnable = false;
		}

		Operation operation;
		int operationCode = mmu.getByte(PC.getValue());

		if (operationCode == PREFIX_CB) {
			PC.inc(1);
			operationCode = mmu.getByte(PC.getValue());
			if (operationsCB.containsKey(operationCode)) {
				operation = operationsCB.get(operationCode);
			} else {
				String message = buildUnimplementedOpcodeMessage(String.format("0xCB 0x%02X", operationCode));
				log.warn(message);
				throw new RuntimeException(message);
			}
		} else if (operations.containsKey(operationCode)) {
			operation = operations.get(operationCode);
		} else {
			String message = buildUnimplementedOpcodeMessage(String.format("0x%02X", operationCode));
			log.warn(message);
			throw new RuntimeException(message);
		}

		int oldPC = PC.getValue();
		operation.execute();

		operation.update(clock);
		if (PC.getValue() == oldPC) {
			PC.inc(operation.getLength());
		}
	}

	private String buildUnimplementedOpcodeMessage(String opcodeHex) {
		int zeroFlag = isFlagSetted(FLAG_ZERO) ? 1 : 0;
		int subtractFlag = isFlagSetted(FLAG_SUBTRACT) ? 1 : 0;
		int halfCarryFlag = isFlagSetted(FLAG_HALF_CARRY) ? 1 : 0;
		int carryFlag = isFlagSetted(FLAG_CARRY) ? 1 : 0;
		return String.format(
				"Unimplemented opcode %s (UNKNOWN) at PC=0x%04X SP=0x%04X AF=0x%04X BC=0x%04X DE=0x%04X HL=0x%04X Flags=[Z=%d N=%d H=%d C=%d]",
				opcodeHex, PC.getValue(), SP.getValue(), AF.getValue(), BC.getValue(), DE.getValue(), HL.getValue(),
				zeroFlag, subtractFlag, halfCarryFlag, carryFlag);
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

    void setPendingInterruptEnable(boolean pendingInterruptEnable) {
		this.pendingInterruptEnable = pendingInterruptEnable;
	}

	public void updateTimers(int cycles) {
		updateDividerRegister(cycles);
		updateTimerRegister(cycles);
	}

	private void updateDividerRegister(int cycles) {
		dividerCounter += cycles;
		while (dividerCounter >= 256) {
			dividerCounter -= 256;
			this.mmu.incrementDividerRegister();
		}
	}

	private void updateTimerRegister(int cycles) {
		if (isClockEnabled()) {
			timerCounter -= cycles;
			while (timerCounter <= 0) {
				if (timerIsAboutToOverflow()) {
					mmu.setByte(MMU.TIMER_ADDRESS, mmu.getByte(MMU.TIMER_MODULATOR));
					mmu.requestInterrupt(MMU.INTERRUPT_TIMER);
				} else {
					mmu.setByte(MMU.TIMER_ADDRESS, mmu.getByte(MMU.TIMER_ADDRESS) + 1);
				}
				timerCounter += convertToTimerCycles(mmu.getByte(MMU.TIMER_CONTROLLER) & 0x03);
			}
		}
	}

	private boolean timerIsAboutToOverflow() {
		return mmu.getByte(MMU.TIMER_ADDRESS) == 255;
	}

	private boolean isClockEnabled() {
		return BitUtils.isBitSetted(mmu.getByte(MMU.TIMER_CONTROLLER), TIMER_ENABLED_BIT);
	}

	public void updateTimerCounter(int value) {
		log.debug("Timer counter updated, value={}", value);
		timerCounter = convertToTimerCycles(value);
	}

	int convertToTimerCycles(int value) {
		switch (value & 0x03) {
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
		int requests = mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS);
		int enabledInterrupts = mmu.getByte(MMU.INTERRUPT_ENABLED_ADDRESS);
		if ((requests & enabledInterrupts) != 0) {
			halted = false;
		}
		if (interruptionsEnabled) {
			for (int i = 0; i < 5; i++) {
				if (canServeInterrupt(requests, enabledInterrupts, i)) {
					log.debug("Serving interrupt bit={}", i);
					serveInterrupt(i);
					return;
				}
			}
		}
	}

	private void serveInterrupt(int bit) {
		interruptionsEnabled = false;
		int value = mmu.getByte(MMU.INTERRUPT_CONTROLLER_ADDRESS);
		mmu.setByte(MMU.INTERRUPT_CONTROLLER_ADDRESS, BitUtils.resetBit(value, bit));

		pushWordOnStack(PC.getValue());
		if (bit != MMU.INTERRUPT_VBLANK) {
			log.debug("Serving non-VBlank interrupt bit={} address=0x{} from PC=0x{}",
					bit, String.format("%04X", INTERRUPT_ADDRESSES.get(bit)),
					String.format("%04X", PC.getValue()));
		}
		PC.setValue(INTERRUPT_ADDRESSES.get(bit));
	}

	public void pushWordOnStack(int value) {
		SP.dec(2);
		if (log.isTraceEnabled()) {
			log.trace("0x{} push] 0x{} {}", Integer.toHexString(PC.getValue()), Integer.toHexString(value), SP);
		}
		mmu.setWord(SP.getValue(), value);
	}

	public int popWordOnStack() {
		int value = mmu.getWord(SP.getValue());
		SP.inc(2);
		if (SP.getValue() > 0xCFFF) {
			log.debug("Stack high-water: pop from 0x{} SP=0x{} value=0x{} PC=0x{}",
					String.format("%04X", SP.getValue() - 2),
					String.format("%04X", SP.getValue()),
					String.format("%04X", value),
					String.format("%04X", PC.getValue()));
		}
		if (log.isTraceEnabled()) {
			log.trace("0x{} pop] 0x{} {}", Integer.toHexString(PC.getValue()), Integer.toHexString(value), SP);
		}
		return value;
	}

	private boolean canServeInterrupt(int requests, int enabledInterrupts, int i) {
		return BitUtils.isBitSetted(requests, i) && BitUtils.isBitSetted(enabledInterrupts, i);
	}

}
