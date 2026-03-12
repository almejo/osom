package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;

class OperationLD_r_r extends Operation {

	private final Register destRegister;
	private final boolean destLo;
	private final Register srcRegister;
	private final boolean srcLo;

	OperationLD_r_r(Z80Cpu cpu, MMU mmu, int code,
					Register destRegister, boolean destLo,
					Register srcRegister, boolean srcLo) {
		super(cpu, mmu, 1, 4, code, 1);
		this.destRegister = destRegister;
		this.destLo = destLo;
		this.srcRegister = srcRegister;
		this.srcLo = srcLo;
	}

	// Flags: - (not affected)
	@Override
	void execute() {
		int value = srcLo ? srcRegister.getLo() : srcRegister.getHi();
		destRegister.set(destLo, value);
	}
}
