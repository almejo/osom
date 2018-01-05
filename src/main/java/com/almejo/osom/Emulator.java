package com.almejo.osom;

import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.gpu.GPU;
import com.almejo.osom.memory.Cartridge;
import com.almejo.osom.memory.MMU;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Emulator {
	private static final int CYCLES = 4194304;
	private static final int CYCLES_PER_FRAME = CYCLES / 60;

	public void run(boolean bootBios, String file) throws IOException {

		Path path = Paths.get(file);
		GPU gpu = new GPU();
		byte[] bytes = Files.readAllBytes(path);
		MMU mmu = new MMU(bootBios, gpu);
		gpu.setMmu(mmu);

		Cartridge cartridge = new Cartridge(bytes);
		mmu.addCartridge(cartridge);
		Z80Cpu cpu = new Z80Cpu(mmu, CYCLES);
		mmu.setCpu(cpu);
		gpu.setCpu(cpu);
		cpu.reset(bootBios);

		JFrame frame = new JFrame();
		frame.setSize(160 * LCDScreen.FACTOR + LCDScreen.FACTOR, 144 * LCDScreen.FACTOR + LCDScreen.FACTOR);
		frame.setPreferredSize(new Dimension(160 * LCDScreen.FACTOR + LCDScreen.FACTOR, 144 * LCDScreen.FACTOR + LCDScreen.FACTOR));
		LCDScreen screen = new LCDScreen(gpu, mmu);
		frame.getContentPane().add(screen);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

//		JTextArea textArea = new JTextArea();
//		textArea.setText(cartridge.toString());
//		textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
//
//		JFrame frame = new JFrame();
//		frame.getContentPane().add(new JScrollPane(textArea));
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.pack();
//		frame.setVisible(true);

		int frameCounter = 0;
		int secondCounter = 0;
		int time = 1000 / 60;
		int totalCycles = 0;
		//noinspection InfiniteLoopStatement
		while (true) {
			long t = System.currentTimeMillis();
			int cyclesToScreen = CYCLES_PER_FRAME;

			while (cyclesToScreen > 0) {
				int oldCycles = cpu.clock.getT();
				//System.out.print(cpu.clock.getT() + " --> ");
				cpu.execute();
				int cycles = cpu.clock.getT() - oldCycles;
				totalCycles += cycles;
				cpu.updateTimers(cycles);
				cpu.checkInterrupts();
				gpu.update(cycles);
				cyclesToScreen -= cycles;
			}
			long delta = System.currentTimeMillis() - t;
			// System.out.println("cycles " + CYCLES_PER_FRAME + " delta " + delta);
			secondCounter += delta;
			frameCounter++;
			screen.setCycles(totalCycles);
			screen.setSeconds(secondCounter);
			screen.setFrameCounter(frameCounter);
			if (delta < 16) {
				try {
					Thread.sleep(16 - delta);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
//			if (secondCounter >= 6000) {
//				System.out.println("--------------------------------------------------frames " + frameCounter);
//				secondCounter = 0;
//				frameCounter = 0;
//				System.exit(0);
//			}
			screen.repaint(time - delta);
		}
	}
}
