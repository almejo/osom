package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationINC_aHL extends Operation {

	OperationINC_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 12, 0x34, 1);
	}

	void execute() {
		int address = cpu.HL.getValue();
		int value = mmu.getByte(address);
		int newValue = (value + 1) & 0xff;
		mmu.setByte(address, newValue);
		if (debug) {
			print("INC [" + cpu.HL.getName() + "] ; HL = "
					+ Integer.toHexString(address)
					+ " " + Integer.toHexString(value)
					+ " --> "
					+ " " + Integer.toHexString(newValue));

		}
		cpu.alu.updateIncFlags(value, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "INC [" + cpu.HL.getName() + "]";
	}
}
