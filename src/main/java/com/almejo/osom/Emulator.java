package com.almejo.osom;

import com.almejo.osom.cpu.ALU;
import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.memory.Cartridge;
import com.almejo.osom.memory.MMU;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Emulator {

	private static final int CYCLES = 10;
	private Z80Cpu cpu;

	public void run(String file) throws IOException {
		// Path path = Paths.get("/home/alejo/git/osom/roms/cpu_instrs.gb");
//		Path path = Paths.get("/home/alejo/git/osom/roms/tetris.gb");
		Path path = Paths.get(file);
		byte[] bytes = Files.readAllBytes(path);
		MMU mmu = new MMU();
		Cartridge cartridge = new Cartridge(bytes);
		System.out.println(cartridge);
		mmu.addCartridge(cartridge);
		cpu = new Z80Cpu();
		cpu.setMmu(mmu);
		cpu.reset();

		while (true) {
			cpu.execute();
		}
	}
}
