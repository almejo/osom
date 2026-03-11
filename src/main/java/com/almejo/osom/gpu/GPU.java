package com.almejo.osom.gpu;

import com.almejo.osom.cpu.BitUtils;
import com.almejo.osom.memory.MMU;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GPU {
	private static final int H_BLANK = 0;
	private static final int V_BLANK = 1;
	private static final int SPRITES = 2;
	private static final int GRAPHICS = 3;

	private static final int OAM_CYCLES = 80;
	private static final int RENDERING_CYCLES = 172;
	private static final int HBLANK_CYCLES = 204;
	private static final int SCANLINE_CYCLES = 456;
	private static final int VISIBLE_LINES = 144;
	private static final int VBLANK_LINES = 10;

	private int line = 0;
	private int clock = 0;
	@Getter
	private int mode = SPRITES;
	@Getter
	@Setter
	private FrameBuffer frameBuffer;
	private MMU mmu;

	public GPU() {
	}

	public void setMmu(MMU mmu) {
		this.mmu = mmu;
		mmu.setScanline(0);
		updateStatMode();
	}

	public void update(int cycles) {
		if (!isEnabled()) {
			return;
		}
		clock += cycles;

		int threshold = currentThreshold();
		while (clock >= threshold) {
			clock -= threshold;

			switch (mode) {
				case SPRITES:
					mode = GRAPHICS;
					break;
				case GRAPHICS:
					drawLine();
					mode = H_BLANK;
					fireStatInterruptIfEnabled();
					break;
				case H_BLANK:
					line++;
					mmu.setScanline(line);
					checkLyCoincidence();
					if (line == VISIBLE_LINES) {
						mode = V_BLANK;
						mmu.requestInterrupt(MMU.INTERRUPT_VBLANK);
					} else {
						mode = SPRITES;
					}
					fireStatInterruptIfEnabled();
					break;
				case V_BLANK:
					line++;
					if (line >= VISIBLE_LINES + VBLANK_LINES) {
						line = 0;
						mode = SPRITES;
						mmu.setScanline(line);
						checkLyCoincidence();
						fireStatInterruptIfEnabled();
					} else {
						mmu.setScanline(line);
						checkLyCoincidence();
					}
					break;
				default:
					break;
			}
			updateStatMode();
			threshold = currentThreshold();
		}
	}

	private void updateStatMode() {
		mmu.setStatModeBits(mode);
	}

	private void fireStatInterruptIfEnabled() {
		int stat = mmu.getByte(MMU.LCD_STATUS);
		boolean shouldFire = false;
		// Pan Docs: STAT bit 3 = H-Blank interrupt, bit 4 = V-Blank interrupt, bit 5 = OAM interrupt
		switch (mode) {
			case H_BLANK:
				shouldFire = BitUtils.isBitSetted(stat, 3);
				break;
			case V_BLANK:
				shouldFire = BitUtils.isBitSetted(stat, 4);
				break;
			case SPRITES:
				shouldFire = BitUtils.isBitSetted(stat, 5);
				break;
			default:
				break;
		}
		if (shouldFire) {
			mmu.requestInterrupt(MMU.INTERRUPT_LCD_STAT);
		}
	}

	private void checkLyCoincidence() {
		int lyc = mmu.getByte(MMU.LCD_LY_COMPARE);
		boolean coincidence = (line == lyc);
		mmu.setStatCoincidenceFlag(coincidence);
		if (coincidence && BitUtils.isBitSetted(mmu.getByte(MMU.LCD_STATUS), 6)) {
			mmu.requestInterrupt(MMU.INTERRUPT_LCD_STAT);
		}
	}

	private int currentThreshold() {
		switch (mode) {
			case SPRITES:
				return OAM_CYCLES;
			case GRAPHICS:
				return RENDERING_CYCLES;
			case H_BLANK:
				return HBLANK_CYCLES;
			case V_BLANK:
				return SCANLINE_CYCLES;
			default:
				return SCANLINE_CYCLES;
		}
	}

	private boolean isEnabled() {
		return BitUtils.isBitSetted(getControlInfo(), 7);
	}

	private int getControlInfo() {
		return mmu.getByte(MMU.LCD_CONTROLLER);
	}

	private void drawLine() {
		int control = getControlInfo();
		if (backgroundEnabled(control)) {
			renderBackground(control);
		}
		if (spritesEnabled(control)) {
			renderSprites();
		}
	}

	private void renderSprites() {
	}

	private boolean spritesEnabled(int control) {
		return BitUtils.isBitSetted(control, 1);
	}

	private void renderBackground(int control) {
		int scrollY = mmu.getByte(MMU.LCD_SCROLL_Y);
		int scrollX = mmu.getByte(MMU.LCD_SCROLL_X);
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

		int posY = (scrollY + line) & 0xFF;
		int tileRow = (posY / 8) * 32;
		for (int pixel = 0; pixel < 160; pixel++) {
			int xPos = (pixel + scrollX) & 0xFF;
			int tileColumn = (xPos / 8);

			int tileAddress = mapLayout + tileRow + tileColumn;
			int tileIndex = useUnsignedIdentifier ? mmu.getByte(tileAddress) : mmu.getByteSigned(tileAddress);
			int tileLocation = tilesData + (tileIndex + (useUnsignedIdentifier ? 0 : 128)) * 16;
			int tileLine = (posY % 8) * 2;
			int byte1 = mmu.getByte(tileLocation + tileLine);
			int byte2 = mmu.getByte(tileLocation + tileLine + 1);
			int bit = xPos % 8;
			bit -= 7;
			bit *= -1;

			int color = BitUtils.isBitSetted(byte1, bit) ? 1 : 0;
			color |= (BitUtils.isBitSetted(byte2, bit) ? 1 : 0) << 1;
			frameBuffer.setPixel(pixel, line, color);
		}
	}

	private boolean backgroundEnabled(int control) {
		return BitUtils.isBitSetted(control, 0);
	}

}
