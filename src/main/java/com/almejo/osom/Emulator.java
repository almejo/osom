package com.almejo.osom;

import com.almejo.osom.cpu.Operation;
import com.almejo.osom.cpu.OperationNotFoundException;
import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.gpu.GPU;
import com.almejo.osom.memory.Cartridge;
import com.almejo.osom.memory.MMU;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class Emulator {
	private static final int CYCLES = 4194304;
	private static final int CYCLES_PER_FRAME = CYCLES / 60;

	void run(boolean bootBios, String file, String dataFile) throws IOException {

		GPU gpu = new GPU();
		MMU mmu = new MMU(bootBios, gpu);
		gpu.setMmu(mmu);
		Cartridge cartridge = new Cartridge(Files.readAllBytes(Paths.get(file)));
		mmu.addCartridge(cartridge);
		Z80Cpu cpu = new Z80Cpu(mmu, CYCLES);
		cpu.setGpu(gpu);
		mmu.setCpu(cpu);
		gpu.setCpu(cpu);
		cpu.reset(bootBios);

		JFrame frame = new JFrame(getConfiguration(2).getDefaultConfiguration());
		frame.setSize(160 * LCDScreen.FACTOR + LCDScreen.FACTOR, 160 * LCDScreen.FACTOR + LCDScreen.FACTOR);
		frame.setPreferredSize(new Dimension(160 * LCDScreen.FACTOR + LCDScreen.FACTOR, 160 * LCDScreen.FACTOR + LCDScreen.FACTOR));
		LCDScreen screen = new LCDScreen(gpu, mmu);
		JButton button = new JButton();
		frame.getContentPane().add(screen, BorderLayout.CENTER);
		frame.getContentPane().add(button, BorderLayout.SOUTH);
		frame.getContentPane().add(new CodeViewer(cpu.operationTable, cartridge, Files.readAllLines(Paths.get(dataFile))));
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mouseEvent) {
				Operation.debug = true;
			}
		});

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
				try {
					cpu.execute();
				} catch (OperationNotFoundException exception) {
					System.out.println(exception.getMessage() + " at 0x" + cpu.PC);
				}
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
			//screen.repaint(time - delta);
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
