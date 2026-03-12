package com.almejo.osom;

import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.gpu.FrameBuffer;
import com.almejo.osom.gpu.GPU;
import com.almejo.osom.input.Joypad;
import com.almejo.osom.memory.Cartridge;
import com.almejo.osom.memory.MMU;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Emulator {
	private static final int CYCLES = 4194304;
	private static final int CYCLES_PER_FRAME = CYCLES / 60;

	private Z80Cpu cpu;
	private GPU gpu;

	@Getter
	private int totalCycles;

	public void initialize(boolean bootBios, String file, FrameBuffer frameBuffer, Joypad joypad, boolean trace) throws IOException {
		if (bootBios) {
			Path biosPath = Paths.get("bios/bios.bin");
			if (!Files.exists(biosPath)) {
				log.error("BIOS file not found at 'bios/bios.bin'. Use --no-bios to skip the boot sequence.");
				throw new IllegalStateException("BIOS file not found at 'bios/bios.bin'. Use --no-bios to skip the boot sequence.");
			}
		}

		Path path = Paths.get(file);
		gpu = new GPU();
		byte[] bytes = Files.readAllBytes(path);
		MMU mmu = new MMU(bootBios);
		mmu.setJoypad(joypad);
		gpu.setMmu(mmu);
		gpu.setFrameBuffer(frameBuffer);

		Cartridge cartridge = new Cartridge(bytes);
		mmu.addCartridge(cartridge);
		cpu = new Z80Cpu(mmu, CYCLES);
		mmu.setCpu(cpu);
		cpu.setTraceEnabled(trace);
		cpu.reset(bootBios);
	}

	public void runFrame() {
		int cyclesToScreen = CYCLES_PER_FRAME;

		while (cyclesToScreen > 0) {
			int oldCycles = cpu.clock.getT();
			cpu.execute();
			int cycles = cpu.clock.getT() - oldCycles;
			totalCycles += cycles;
			cpu.updateTimers(cycles);
			cpu.checkInterrupts();
			gpu.update(cycles);
			cyclesToScreen -= cycles;
		}
	}
}
