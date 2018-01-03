package com.almejo.osom.gpu;

import com.almejo.osom.cpu.BitUtils;
import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.memory.MMU;

public class GPU {
	private static final int H_BLANK = 0;
	private static final int V_BLANK = 1;
	private static final int SPRITES = 2;
	private static final int GRAPHICS = 3;

	private int line = 0;
	private int clock = 0;
	private int mode = H_BLANK;
	private static final int[][][] tiles = new int[512][8][8];
	private static final int[][] pixels = new int[160][144];
	private Z80Cpu cpu;
	private MMU mmu;

	public GPU() {
	}

	public int[][] getPixels() {
		return pixels;
	}

	public void setCpu(Z80Cpu cpu) {
		this.cpu = cpu;
	}

	public void setMmu(MMU mmu) {
		this.mmu = mmu;
		mmu.setScanline(1);
	}

	public void update(int cycles) {
		if (!isEnabled()) {
			return;
		}
		clock += cycles;
		switch (mode) {
			case H_BLANK:
				if (clock >= 204) {
					clock = 0;
					line++;
					mmu.setScanline(line + 1);
					if (line == 144) {
						mode = V_BLANK;
						cpu.requestInterrupt(Z80Cpu.INTERRUPT_ADDRESS_V_BLANK);
						drawScreen();
					} else {
						mode = SPRITES;
					}
				}
				break;
			case V_BLANK:
//				if (clock >= 456 * 144) {
//					clock = 0;
//					line = 0;
//					mmu.setScanline(1);
//					mode = SPRITES;
//				}
				if (clock >= 456) {
					clock = 0;
					line++;
					mmu.setScanline(line + 1);
					if (line > 153) {
						mode = SPRITES;
						line = 0;
						mmu.setScanline(line + 1);
					}
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

	private boolean isEnabled() {
		return BitUtils.isBitSetted(getControlInfo(), 7);
	}

	private int getControlInfo() {
		return mmu.getByte(MMU.LCD_CONTROLLER);
	}

	private void drawLine() {
		if (mmu.getByte(MMU.LCD_LINE_COUNTER) > 143) {
			return;
		}
		System.out.println("draw drawLine");
		int control = getControlInfo();
		if (backgroundEnabled(control)) {
			renderBackground(control);
		}
		if (spritesEnabled(control)) {
			renderSprites();
		}
	}

	private void drawScreen() {

	}

	private void renderSprites() {
	}

	private boolean spritesEnabled(int control) {
		return BitUtils.isBitSetted(control, 1);
	}

	private void renderBackground(int control) {
		int scrollY = mmu.getByte(0xFF43);
		int scrollX = mmu.getByte(0xFF42);
		int line = mmu.getByte(MMU.LCD_LINE_COUNTER);
		int tilesData;
		boolean useUnsignedIdentifier = true;
		int mapLayout;
		if (BitUtils.isBitSetted(control, 4)) {
			tilesData = 0x8000;
		} else {
			tilesData = 0x8800;
			useUnsignedIdentifier = false;
		}
		if (BitUtils.isBitSetted(control, 3)) {
			mapLayout = 0x9C00;
		} else {
			mapLayout = 0x9800;
		}

		int posY = scrollY + line;
		int tileRow = (posY / 8) * 32;
		for (int pixel = 0; pixel < 160; pixel++) {
			int xPos = pixel + scrollX;
			// which of the 32 horizontal tiles does this xPos fall within?
			int tileColumn = (xPos / 8);

			// get the tile identity number. Remember it can be signed
			// or unsigned
			int tileAddress = mapLayout + tileRow + tileColumn;
			int tileIndex = useUnsignedIdentifier ? mmu.getByte(tileAddress) : mmu.getByteSigned(tileAddress);
//			if (useUnsignedIdentifier)
//				tileIndex = (BYTE) ReadMemory(tileAddress);
//			else
//				tileIndex = (SIGNED_BYTE) ReadMemory(tileAddress);
			int tileLocation = tilesData + (tileIndex + (useUnsignedIdentifier ? 0 : 128)) * 16;
			int tileLine = (line % 8) * 2;// 2 bytes
			int byte1 = mmu.getByte(tileLocation + tileLine);
			int byte2 = mmu.getByte(tileLocation + tileLine + 1);
			int bit = xPos % 8;
			bit -= 7;
			bit *= -1;

			int color = BitUtils.isBitSetted(byte1, bit) ? 1 : 0;
			color |= (BitUtils.isBitSetted(byte2, bit) ? 1 : 0) << 1;
			pixels[pixel][line] = color;
		}
	}

	private boolean backgroundEnabled(int control) {
		return BitUtils.isBitSetted(control, 0);
	}

	public void updateTile(int tile, int y, int x, int i) {
		tiles[tile][x][y] = i;
	}
}
