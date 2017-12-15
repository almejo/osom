package com.almejo.osom.cpu;


import com.almejo.osom.memory.MMU;
import lombok.Getter;

public abstract class Operation {
	protected Z80Cpu cpu;

	protected MMU mmu;
	@Getter
	protected int code;

	@Getter
	private int length;

	Operation(Z80Cpu cpu, MMU mmu, int code, int length) {
		this.cpu = cpu;
		this.mmu = mmu;
		this.code = code;
		this.length = length;
	}

	abstract void execute();
}