package com.almejo.osom;

import com.almejo.osom.gpu.GPU;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class LCDScreen extends JPanel {
	private final GPU gpu;

	static final int FACTOR = 2;
	private int seconds;
	private int frameCounter;
	private int cycles;

	LCDScreen(GPU gpu) {
		setSize(new Dimension(160 * FACTOR, 144 * FACTOR));
		this.gpu = gpu;
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D graphics = (Graphics2D) g;
		graphics.setColor(Color.yellow);
		graphics.drawRect(0, 0, 160 * FACTOR, 144 * FACTOR);

		int[][] pixels = gpu.getPixels();
		for (int y = 0; y < 144; y++) {
			for (int x = 0; x < 160; x++) {
				Color color = getColor(pixels[x][y]);
				graphics.setColor(color);
				graphics.fillRect(x * FACTOR, y * FACTOR, FACTOR, FACTOR);
			}
		}
		graphics.setColor(Color.red);
		graphics.drawString("t: " + System.currentTimeMillis(), 0, 10);
		graphics.drawString("c: " + cycles, 0, 30);
		graphics.drawString("f: " + frameCounter, 0, 40);
		graphics.drawString("s: " + seconds, 0, 50);

		repaint();
	}

	private Color getColor(int i) {
		switch (i) {
			case 0:
				return Color.black;
			case 1:
				return Color.green;
			case 2:
				return Color.red;
			case 3:
				return Color.blue;
		}
		return Color.white;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}

	public void setFrameCounter(int frameCounter) {
		this.frameCounter = frameCounter;
	}

	public void setCycles(int cycles) {
		this.cycles = cycles;
	}
}
