package com.almejo.osom.cpu;

import com.almejo.osom.memory.MMU;
import lombok.Getter;

public abstract class Operation {

	protected final Z80Cpu cpu;
	protected final MMU mmu;
	@Getter
	protected final int code;
	@Getter
	private final int length;
	@Getter
	protected final int t;
	protected final int m;

	Operation(Z80Cpu cpu, MMU mmu, int m, int t, int code, int length) {
		this.cpu = cpu;
		this.mmu = mmu;
		this.t = t;
		this.m = m;
		this.code = code;
		this.length = length;
	}

	void update(Clock clock) {
		clock.update(m, t);
	}

	abstract void execute();
}
