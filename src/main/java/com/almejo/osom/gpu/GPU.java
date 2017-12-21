package com.almejo.osom.gpu;

public class GPU {
	private static final int H_BLANK = 0;
	private static final int V_BLANK = 1;
	private static final int SPRITES = 2;
	private static final int GRAPHICS = 3;

	private int line = 0;
	private int clock = 0;
	private int mode = H_BLANK;
	private static final int[][][] tiles = new int[512][8][8];

	public GPU() {
	}

	public void update(int cycles) {
		clock += cycles;
		switch (mode) {
			case H_BLANK:
				if (clock >= 204) {
					clock = 0;
					line++;

					if (line >= 143) {
						mode = V_BLANK;
						drawScreen();
					} else {
						mode = SPRITES;
					}
				}
				break;
			case V_BLANK:
				if (clock >= 456 * 144) {
					clock = 0;
					line = 0;
					mode = SPRITES;
				}
				break;
			case SPRITES:
				if (clock >= 80) {
					clock = 0;
					mode = GRAPHICS;
				}
				break;
			case GRAPHICS:
				if (clock >= 172) {
					clock = 0;
					mode = H_BLANK;
					drawLine();
				}
		}
	}

	private void drawLine() {
		System.out.println("drawLine");
	}

	private void drawScreen() {
		System.out.println("draw screeeeeeeeeeeeeeeeeeeen");
	}

	public void updateTile(int tile, int y, int x, int i) {
		tiles[tile][x][y] = i;
	}
}
