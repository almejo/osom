package com.almejo.osom.gpu;

import com.almejo.osom.cpu.BitUtils;
import com.almejo.osom.memory.MMU;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

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
	private static final int SCREEN_WIDTH = 160;

	private static final int OAM_ENTRY_COUNT = 40;
	private static final int OAM_BASE_ADDRESS = 0xFE00;
	private static final int OAM_ENTRY_SIZE = 4;
	private static final int MAX_SPRITES_PER_LINE = 10;
	private static final int SPRITE_TILE_DATA_BASE = 0x8000;
	private static final int BYTES_PER_TILE = 16;
	private static final int SPRITE_Y_OFFSET = 16;
	private static final int SPRITE_X_OFFSET = 8;

	private int line = 0;
	private int clock = 0;
	private boolean wasEnabled = false;
	@Getter
	private int mode = SPRITES;
	@Getter
	@Setter
	private FrameBuffer frameBuffer;
	private MMU mmu;
	private final int[] backgroundColorIndices = new int[SCREEN_WIDTH];

	public GPU() {
	}

	public void setMmu(MMU mmu) {
		this.mmu = mmu;
		mmu.setScanline(0);
		updateStatMode();
	}

	public void update(int cycles) {
		boolean enabled = isEnabled();
		if (!enabled) {
			if (wasEnabled) {
				log.info("GPU disabled — resetting LY to 0 (was line={}, mode={})", line, mode);
				line = 0;
				clock = 0;
				mode = SPRITES;
				mmu.setScanline(0);
				updateStatMode();
			}
			wasEnabled = false;
			return;
		}
		if (!wasEnabled) {
			log.info("GPU enabled — starting from line=0, LCDC=0x{}", String.format("%02X", getControlInfo()));
		}
		wasEnabled = true;
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
		Arrays.fill(backgroundColorIndices, 0);
		if (backgroundEnabled(control)) {
			renderBackground(control);
		}
		if (spritesEnabled(control)) {
			renderSprites();
		}
	}

	private void renderSprites() {
		int control = getControlInfo();
		int spriteHeight = getSpriteHeight(control);
		int spritesOnLine = 0;

		for (int sprite = 0; sprite < OAM_ENTRY_COUNT; sprite++) {
			if (spritesOnLine >= MAX_SPRITES_PER_LINE) {
				break;
			}

			int oamAddress = OAM_BASE_ADDRESS + sprite * OAM_ENTRY_SIZE;
			int yPosition = mmu.getByte(oamAddress);
			int xPosition = mmu.getByte(oamAddress + 1);
			int tileIndex = mmu.getByte(oamAddress + 2);
			int attributes = mmu.getByte(oamAddress + 3);

			int spriteY = yPosition - SPRITE_Y_OFFSET;
			if (!isSpriteOnScanline(spriteY, spriteHeight)) {
				continue;
			}

			spritesOnLine++;

			boolean xFlip = BitUtils.isBitSetted(attributes, 5);
			boolean yFlip = BitUtils.isBitSetted(attributes, 6);
			boolean bgPriority = BitUtils.isBitSetted(attributes, 7);
			boolean usePalette1 = BitUtils.isBitSetted(attributes, 4);

			int tileRow = resolveSpriteTileRow(line - spriteY, spriteHeight, yFlip);
			int[] adjusted = adjustTileIndexForDoubleHeight(tileIndex, tileRow, spriteHeight);
			tileIndex = adjusted[0];
			tileRow = adjusted[1];

			int tileDataAddress = SPRITE_TILE_DATA_BASE + tileIndex * BYTES_PER_TILE + tileRow * 2;
			int byte1 = mmu.getByte(tileDataAddress);
			int byte2 = mmu.getByte(tileDataAddress + 1);

			for (int column = 0; column < 8; column++) {
				int screenX = xPosition - SPRITE_X_OFFSET + column;
				if (isPixelOffScreen(screenX)) {
					continue;
				}

				int bit = xFlip ? column : 7 - column;
				int colorIndex = decodeTileColorIndex(byte1, byte2, bit);
				renderSpritePixel(screenX, colorIndex, bgPriority, usePalette1);
			}
		}
	}

	int getSpriteHeight(int control) {
		return BitUtils.isBitSetted(control, 2) ? 16 : 8;
	}

	boolean isSpriteOnScanline(int spriteY, int spriteHeight) {
		return line >= spriteY && line < spriteY + spriteHeight;
	}

	boolean isPixelOffScreen(int screenX) {
		return screenX < 0 || screenX >= SCREEN_WIDTH;
	}

	boolean isHiddenBehindBackground(boolean bgPriority, int screenX) {
		return bgPriority && backgroundColorIndices[screenX] != 0;
	}

	int resolveSpriteTileRow(int tileRow, int spriteHeight, boolean yFlip) {
		if (yFlip) {
			return spriteHeight - 1 - tileRow;
		}
		return tileRow;
	}

	int[] adjustTileIndexForDoubleHeight(int tileIndex, int tileRow, int spriteHeight) {
		if (spriteHeight == 16) {
			if (tileRow >= 8) {
				return new int[]{tileIndex | 0x01, tileRow - 8};
			} else {
				return new int[]{tileIndex & 0xFE, tileRow};
			}
		}
		return new int[]{tileIndex, tileRow};
	}

	int decodeTileColorIndex(int byte1, int byte2, int bit) {
		int colorIndex = BitUtils.isBitSetted(byte1, bit) ? 1 : 0;
		colorIndex |= (BitUtils.isBitSetted(byte2, bit) ? 1 : 0) << 1;
		return colorIndex;
	}

	void renderSpritePixel(int screenX, int colorIndex, boolean bgPriority, boolean usePalette1) {
		if (colorIndex == 0) {
			return;
		}

		if (isHiddenBehindBackground(bgPriority, screenX)) {
			return;
		}

		int palette = usePalette1 ? mmu.getByte(MMU.PALETTE_OBP1) : mmu.getByte(MMU.PALETTE_OBP0);
		int shade = (palette >> (colorIndex * 2)) & 0x03;
		frameBuffer.setPixel(screenX, line, shade);
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

			int colorIndex = decodeTileColorIndex(byte1, byte2, bit);
			backgroundColorIndices[pixel] = colorIndex;
			int bgp = mmu.getByte(MMU.PALETTE_BGP);
			int shade = (bgp >> (colorIndex * 2)) & 0x03;
			frameBuffer.setPixel(pixel, line, shade);
		}
	}

	private boolean backgroundEnabled(int control) {
		return BitUtils.isBitSetted(control, 0);
	}

}
