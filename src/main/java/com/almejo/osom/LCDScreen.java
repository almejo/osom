package com.almejo.osom;

import com.almejo.osom.gpu.GPU;

import javax.swing.*;
import java.awt.*;

public class LCDScreen extends JPanel {
	private final GPU gpu;

	private static final int FACTOR = 3;

	public LCDScreen(GPU gpu) {
		setSize(new Dimension(160 * FACTOR, 144 * FACTOR));
		this.gpu = gpu;
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		System.out.println("=====================================================================================");
		g2.setColor(Color.yellow);
		g2.drawRect(0, 0, 160 * FACTOR, 144 * FACTOR);
		g2.setColor(Color.red);
		g2.drawString("t: " +System.currentTimeMillis(), 0, 20);
		int[][] pixels = gpu.getPixels();
		for (int y = 0; y < 144; y++) {
			for (int x = 0; x < 160; x++) {
				Color color = getColor(pixels[x][y]);
				g2.setColor(color);
				g2.fillRect(x * FACTOR, y * FACTOR, FACTOR, FACTOR);
			}
		}
	}

	private Color getColor(int i) {
		switch (i) {
			case 0:
				return Color.black;
			case 1:
				return Color.white;
			case 2:
				return Color.red;
			case 3:
				return Color.blue;
		}
		return Color.green;
	}
}
