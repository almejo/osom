package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationSTOP extends Operation {

	OperationSTOP(Z80Cpu cpu, MMU mmu) {
		super(cpu, mmu, 1, 4, 0x10, 2);
	}

	// Flags: - (not affected)
	// Known gap: STOP should halt CPU and LCD until a button is pressed,
	// switch to low-power mode, and on CGB switch CPU speed. Currently a no-op stub.
	@Override
	void execute() {
		// No-op: STOP halts until a button is pressed — not yet implemented
	}
}
