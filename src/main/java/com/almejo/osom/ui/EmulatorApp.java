package com.almejo.osom.ui;

import com.almejo.osom.Emulator;
import com.almejo.osom.gpu.FrameBuffer;
import com.almejo.osom.input.Joypad;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

@Slf4j
public class EmulatorApp {

	private volatile boolean running = true;

	public void run(boolean bootBios, String file) throws IOException {
		FrameBuffer frameBuffer = new FrameBuffer();
		Joypad joypad = new Joypad();
		Emulator emulator = new Emulator();
		emulator.initialize(bootBios, file, frameBuffer, joypad);

		LCDScreen lcdScreen = new LCDScreen(frameBuffer);
		lcdScreen.setJoypad(joypad);

		int frameWidth = FrameBuffer.WIDTH * LCDScreen.FACTOR + LCDScreen.FACTOR;
		int frameHeight = FrameBuffer.HEIGHT * LCDScreen.FACTOR + LCDScreen.FACTOR + LCDScreen.INDICATOR_STRIP_HEIGHT;

		JFrame frame = new JFrame(getConfiguration(2).getDefaultConfiguration());
		frame.setSize(frameWidth, frameHeight);
		frame.setPreferredSize(new Dimension(frameWidth, frameHeight));
		frame.getContentPane().add(lcdScreen, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				running = false;
			}
		});
		frame.pack();
		frame.setVisible(true);
		lcdScreen.requestFocusInWindow();

		int frameCounter = 0;
		long secondCounter = 0;

		while (running) {
			long startTime = System.currentTimeMillis();
			emulator.runFrame();
			long delta = System.currentTimeMillis() - startTime;
			secondCounter += delta;
			frameCounter++;

			lcdScreen.setCycles(emulator.getTotalCycles());
			lcdScreen.setSeconds(secondCounter);
			lcdScreen.setFrameCounter(frameCounter);
			lcdScreen.repaint();

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
		GraphicsDevice[] screens = environment.getScreenDevices();
		if (monitor > -1 && monitor < screens.length) {
			return screens[Math.min(monitor, screens.length)];
		}
		if (screens.length > 0) {
			return screens[0];
		}
		throw new RuntimeException("No Screens Found");
	}
}
