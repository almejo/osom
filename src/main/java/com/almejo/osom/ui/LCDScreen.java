package com.almejo.osom.ui;

import com.almejo.osom.gpu.FrameBuffer;
import lombok.Setter;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class LCDScreen extends JPanel {
	private final FrameBuffer frameBuffer;

	static final int FACTOR = 2;
	@Setter
	private long seconds;
	@Setter
	private int frameCounter;
	@Setter
	private int cycles;

	LCDScreen(FrameBuffer frameBuffer) {
		setSize(new Dimension(FrameBuffer.WIDTH * FACTOR, FrameBuffer.HEIGHT * FACTOR));
		this.frameBuffer = frameBuffer;
	}

	@Override
	public void paint(Graphics graphics) {
		Graphics2D graphics2D = (Graphics2D) graphics;
		graphics2D.setColor(Color.yellow);
		graphics2D.drawRect(0, 0, FrameBuffer.WIDTH * FACTOR, FrameBuffer.HEIGHT * FACTOR);

		int[][] pixels = frameBuffer.getPixels();
		for (int y = 0; y < FrameBuffer.HEIGHT; y++) {
			for (int x = 0; x < FrameBuffer.WIDTH; x++) {
				Color color = getColor(pixels[x][y]);
				graphics2D.setColor(color);
				graphics2D.fillRect(x * FACTOR, y * FACTOR, FACTOR, FACTOR);
			}
		}
		graphics2D.setColor(Color.red);
		graphics2D.drawString("t: " + System.currentTimeMillis(), 0, 10);
		graphics2D.drawString("c: " + cycles, 0, 30);
		graphics2D.drawString("f: " + frameCounter, 0, 40);
		graphics2D.drawString("s: " + seconds, 0, 50);
	}

	private Color getColor(int colorIndex) {
		return switch (colorIndex) {
			case 0 -> Color.black;
			case 1 -> Color.green;
			case 2 -> Color.red;
			case 3 -> Color.blue;
			default -> Color.white;
		};
	}

}
