package com.almejo.osom;

public class Emulator {

	private static final int CYCLES = 10;
	private DataBus bus = new DataBus();
	private Z80Cpu cpu = new Z80Cpu(bus);

	public void run() {
		while (true) {
			cpu.execute(CYCLES);
		}
	}
}
