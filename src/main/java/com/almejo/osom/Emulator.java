package com.almejo.osom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Emulator {

	private static final int CYCLES = 10;
	private DataBus bus = new DataBus();
	private Z80Cpu cpu = new Z80Cpu(bus);

	public void run() throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get("/home/alejo/git/osom/roms/cpu_instrs.gb"));
		System.out.println(bytes);

		for (byte b : bytes) {
			Integer i = b < 0 ? 256 + b: b;
			System.out.println(Integer.toHexString(i));

		}
//		while (true) {
//			cpu.execute(CYCLES);
//		}
	}
}
