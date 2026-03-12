package com.almejo.osom.ui;

import com.almejo.osom.gpu.FrameBuffer;
import com.almejo.osom.input.Joypad;
import lombok.Setter;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class LCDScreen extends JPanel implements KeyListener {
	private final FrameBuffer frameBuffer;

	static final int FACTOR = 2;
	static final int INDICATOR_STRIP_HEIGHT = 20;
	private static final int DOT_RADIUS = 6;

	private static final Color COLOR_UP = Color.GREEN;
	private static final Color COLOR_DOWN = Color.RED;
	private static final Color COLOR_LEFT = Color.BLUE;
	private static final Color COLOR_RIGHT = Color.YELLOW;
	private static final Color COLOR_A = Color.MAGENTA;
	private static final Color COLOR_B = Color.CYAN;
	private static final Color COLOR_START = Color.WHITE;
	private static final Color COLOR_SELECT = Color.ORANGE;
	@Setter
	private long seconds;
	@Setter
	private int frameCounter;
	@Setter
	private int cycles;
	@Setter
	private Joypad joypad;

	LCDScreen(FrameBuffer frameBuffer) {
		this.frameBuffer = frameBuffer;
		addKeyListener(this);
		setFocusable(true);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(FrameBuffer.WIDTH * FACTOR, FrameBuffer.HEIGHT * FACTOR + INDICATOR_STRIP_HEIGHT);
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

		if (joypad != null) {
			drawButtonIndicators(graphics2D);
		}
	}

	private void drawButtonIndicators(Graphics2D graphics2D) {
		int stripTop = FrameBuffer.HEIGHT * FACTOR;
		graphics2D.setColor(Color.BLACK);
		graphics2D.fillRect(0, stripTop, FrameBuffer.WIDTH * FACTOR, INDICATOR_STRIP_HEIGHT);

		int stripCenterY = stripTop + INDICATOR_STRIP_HEIGHT / 2;
		int diameter = DOT_RADIUS * 2;
		int dotSpacing = diameter + 4;

		// D-pad group on the left
		int dpadStartX = 20;
		drawDot(graphics2D, dpadStartX, stripCenterY, diameter, COLOR_UP, Joypad.BUTTON_UP);
		drawDot(graphics2D, dpadStartX + dotSpacing, stripCenterY, diameter, COLOR_DOWN, Joypad.BUTTON_DOWN);
		drawDot(graphics2D, dpadStartX + dotSpacing * 2, stripCenterY, diameter, COLOR_LEFT, Joypad.BUTTON_LEFT);
		drawDot(graphics2D, dpadStartX + dotSpacing * 3, stripCenterY, diameter, COLOR_RIGHT, Joypad.BUTTON_RIGHT);

		// Action group on the right
		int actionStartX = FrameBuffer.WIDTH * FACTOR - 100;
		drawDot(graphics2D, actionStartX, stripCenterY, diameter, COLOR_SELECT, Joypad.BUTTON_SELECT);
		drawDot(graphics2D, actionStartX + dotSpacing * 2, stripCenterY, diameter, COLOR_START, Joypad.BUTTON_START);
		drawDot(graphics2D, actionStartX + dotSpacing * 4, stripCenterY, diameter, COLOR_B, Joypad.BUTTON_B);
		drawDot(graphics2D, actionStartX + dotSpacing * 5, stripCenterY, diameter, COLOR_A, Joypad.BUTTON_A);
	}

	private void drawDot(Graphics2D graphics2D, int centerX, int centerY, int diameter, Color color, int button) {
		int x = centerX - diameter / 2;
		int y = centerY - diameter / 2;
		if (joypad.isButtonPressed(button)) {
			graphics2D.setColor(color);
			graphics2D.fillOval(x, y, diameter, diameter);
		} else {
			graphics2D.setColor(Color.DARK_GRAY);
			graphics2D.drawOval(x, y, diameter, diameter);
		}
	}

	@Override
	public void keyPressed(KeyEvent keyEvent) {
		if (joypad != null) {
			joypad.keyPressed(keyEvent.getKeyCode());
		}
	}

	@Override
	public void keyReleased(KeyEvent keyEvent) {
		if (joypad != null) {
			joypad.keyReleased(keyEvent.getKeyCode());
		}
	}

	@Override
	public void keyTyped(KeyEvent keyEvent) {
	}

	private static final Color[] DMG_SHADES = {
		new Color(155, 188, 15),  // Shade 0 — lightest
		new Color(139, 172, 15),  // Shade 1 — light
		new Color(48, 98, 48),    // Shade 2 — dark
		new Color(15, 56, 15)     // Shade 3 — darkest
	};

	private Color getColor(int shade) {
		if (shade >= 0 && shade <= 3) {
			return DMG_SHADES[shade];
		}
		return Color.white;
	}

}
