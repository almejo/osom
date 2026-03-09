package com.almejo.osom;

import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.gpu.GPU;
import com.almejo.osom.memory.Cartridge;
import com.almejo.osom.memory.MMU;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class Emulator {
	private static final int CYCLES = 4194304;
	private static final int CYCLES_PER_FRAME = CYCLES / 60;

	public void run(boolean bootBios, String file) throws IOException {
		if (bootBios) {
			Path biosPath = Paths.get("bios/bios.bin");
			if (!Files.exists(biosPath)) {
				log.error("BIOS file not found at 'bios/bios.bin'. Use --no-bios to skip the boot sequence.");
				throw new IllegalStateException("BIOS file not found at 'bios/bios.bin'. Use --no-bios to skip the boot sequence.");
			}
		}

		Path path = Paths.get(file);
		GPU gpu = new GPU();
		byte[] bytes = Files.readAllBytes(path);
		MMU mmu = new MMU(bootBios);
		gpu.setMmu(mmu);

		Cartridge cartridge = new Cartridge(bytes);
		mmu.addCartridge(cartridge);
		Z80Cpu cpu = new Z80Cpu(mmu, CYCLES);
		mmu.setCpu(cpu);
		gpu.setCpu(cpu);
		cpu.reset(bootBios);

		JFrame frame = new JFrame(getConfiguration(2).getDefaultConfiguration());
		frame.setSize(160 * LCDScreen.FACTOR + LCDScreen.FACTOR, 160 * LCDScreen.FACTOR + LCDScreen.FACTOR);
		frame.setPreferredSize(new Dimension(160 * LCDScreen.FACTOR + LCDScreen.FACTOR, 160 * LCDScreen.FACTOR + LCDScreen.FACTOR));
		LCDScreen screen = new LCDScreen(gpu);
		frame.getContentPane().add(screen, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		int frameCounter = 0;
		int secondCounter = 0;
		int totalCycles = 0;
		//noinspection InfiniteLoopStatement
		while (true) {
			long t = System.currentTimeMillis();
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
			long delta = System.currentTimeMillis() - t;
			secondCounter += delta;
			frameCounter++;
			screen.setCycles(totalCycles);
			screen.setSeconds(secondCounter);
			screen.setFrameCounter(frameCounter);
			if (secondCounter >= 1000) {
				log.info("Frames: {}, FPS: {}", frameCounter, frameCounter * 1000 / secondCounter);
				secondCounter = 0;
				frameCounter = 0;
			}
			if (delta < 16) {
				try {
					Thread.sleep(16 - delta);
				} catch (InterruptedException exception) {
					log.warn("Emulation thread interrupted", exception);
				}
			}
		}
	}

	private GraphicsDevice getConfiguration(int monitor) {
		GraphicsEnvironment environment = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = environment.getScreenDevices();
		if (monitor > -1 && monitor < gs.length) {
			return gs[Math.min(monitor, gs.length)];
		}
		if (gs.length > 0) {
			return gs[0];
		}
		throw new RuntimeException("No Screens Found");

	}
}
