package com.almejo.osom.gpu;

import lombok.Getter;

public class FrameBuffer {
	public static final int WIDTH = 160;
	public static final int HEIGHT = 144;

	@Getter
	private final int[][] pixels = new int[WIDTH][HEIGHT];

	public void setPixel(int x, int y, int color) {
		pixels[x][y] = color;
	}
}
