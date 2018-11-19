package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationDEC_aHL extends Operation {

	OperationDEC_aHL(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 12, 0x35, 1);
	}

	@Override
	void execute() {
		int address = cpu.HL.getValue();
		int value = mmu.getByte(address);
		int newValue = (value - 1) & 0xff;
		mmu.setByte(address, newValue);
		if (debug) {
			print("DEC [" + cpu.HL.getName() + "] ; HL = "
					+ Integer.toHexString(address)
					+ " " + Integer.toHexString(value)
					+ " --> "
					+ " " + Integer.toHexString(newValue));

		}
		cpu.alu.updateDecFlags(value, 1);
	}

	@Override
	public String decoded(int[] ram, int base) {
		return "DEC [" + cpu.HL.getName() + "]";
	}
}
