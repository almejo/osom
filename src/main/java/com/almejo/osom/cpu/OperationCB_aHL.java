package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationCB_aHL extends OperationCB {

	private final int operation;
	private final int bit;

	static final int OP_RLC = 0;
	static final int OP_RRC = 1;
	static final int OP_RL = 2;
	static final int OP_RR = 3;
	static final int OP_SLA = 4;
	static final int OP_SRA = 5;
	static final int OP_SWAP = 6;
	static final int OP_SRL = 7;
	static final int OP_BIT = 8;
	static final int OP_RES = 9;
	static final int OP_SET = 10;

	OperationCB_aHL(Z80Cpu cpu, MMU mmu, int code, int operation, int bit) {
		super(cpu, mmu, 2, operation == OP_BIT ? 12 : 16, code, 1);
		this.operation = operation;
		this.bit = bit;
	}

	// Flags: varies by operation (see switch cases)
	@Override
	void execute() {
		int address = cpu.HL.getValue();
		int value = mmu.getByte(address);
		switch (operation) {
			case OP_RLC: {
				int carry = (value >> 7) & 1;
				int result = ((value << 1) | carry) & 0xFF;
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, carry == 1);
				break;
			}
			case OP_RRC: {
				int carry = value & 1;
				int result = ((value >> 1) | (carry << 7)) & 0xFF;
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, carry == 1);
				break;
			}
			case OP_RL: {
				int oldCarry = cpu.isFlagSetted(Z80Cpu.FLAG_CARRY) ? 1 : 0;
				int result = ((value << 1) | oldCarry) & 0xFF;
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, (value & 0x80) != 0);
				break;
			}
			case OP_RR: {
				int oldCarry = cpu.isFlagSetted(Z80Cpu.FLAG_CARRY) ? 1 : 0;
				int result = ((value >> 1) | (oldCarry << 7)) & 0xFF;
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, (value & 1) == 1);
				break;
			}
			case OP_SLA: {
				int result = (value << 1) & 0xFF;
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, (value & 0x80) != 0);
				break;
			}
			case OP_SRA: {
				int result = ((value >> 1) | (value & 0x80)) & 0xFF;
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, (value & 1) == 1);
				break;
			}
			case OP_SWAP: {
				int result = ((value & 0xF0) >> 4) | ((value & 0x0F) << 4);
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, false);
				break;
			}
			case OP_SRL: {
				int result = (value >> 1) & 0xFF;
				mmu.setByte(address, result);
				cpu.setFlag(Z80Cpu.FLAG_ZERO, result == 0);
				cpu.setFlag(Z80Cpu.FLAG_SUBTRACT, false);
				cpu.setFlag(Z80Cpu.FLAG_HALF_CARRY, false);
				cpu.setFlag(Z80Cpu.FLAG_CARRY, (value & 1) == 1);
				break;
			}
			case OP_BIT: {
				cpu.alu.setBITFlags((value & (1 << bit)) == 0);
				break;
			}
			case OP_RES: {
				mmu.setByte(address, BitUtils.resetBit(value, bit));
				break;
			}
			case OP_SET: {
				mmu.setByte(address, BitUtils.setBit(value, bit));
				break;
			}
		}
	}
}
