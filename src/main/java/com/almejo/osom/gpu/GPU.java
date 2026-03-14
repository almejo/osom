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

	private static final int TILE_DATA_UNSIGNED_BASE = 0x8000;
	private static final int TILE_DATA_SIGNED_BASE = 0x8800;
	private static final int TILE_MAP_LOW = 0x9800;
	private static final int TILE_MAP_HIGH = 0x9C00;
	private static final int BYTES_PER_TILE = 16;
	private static final int BYTES_PER_TILE_ROW = 2;
	private static final int TILE_WIDTH_PIXELS = 8;
	private static final int TILE_MAP_ROW_LENGTH = 32;
	private static final int SIGNED_TILE_INDEX_OFFSET = 128;

	private static final int LCDC_BG_ENABLE = 0;
	private static final int LCDC_SPRITE_ENABLE = 1;
	private static final int LCDC_SPRITE_SIZE = 2;
	private static final int LCDC_BG_TILE_MAP = 3;
	private static final int LCDC_TILE_DATA = 4;
	private static final int LCDC_WINDOW_ENABLE = 5;
	private static final int LCDC_WINDOW_TILE_MAP = 6;
	private static final int LCDC_LCD_ENABLE = 7;

	private static final int STAT_HBLANK_INTERRUPT = 3;
	private static final int STAT_VBLANK_INTERRUPT = 4;
	private static final int STAT_OAM_INTERRUPT = 5;
	private static final int STAT_LYC_INTERRUPT = 6;

	private static final int SPRITE_ATTR_PALETTE = 4;
	private static final int SPRITE_ATTR_X_FLIP = 5;
	private static final int SPRITE_ATTR_Y_FLIP = 6;
	private static final int SPRITE_ATTR_BG_PRIORITY = 7;

	private static final int OAM_ENTRY_COUNT = 40;
	private static final int OAM_BASE_ADDRESS = 0xFE00;
	private static final int OAM_ENTRY_SIZE = 4;
	private static final int MAX_SPRITES_PER_LINE = 10;
	private static final int SPRITE_Y_OFFSET = 16;
	private static final int SPRITE_X_OFFSET = 8;

	private static final int WINDOW_X_OFFSET = 7;

	private int line = 0;
	private int clock = 0;
	private int windowLineCounter = 0;
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
						windowLineCounter = 0;
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
				shouldFire = BitUtils.isBitSetted(stat, STAT_HBLANK_INTERRUPT);
				break;
			case V_BLANK:
				shouldFire = BitUtils.isBitSetted(stat, STAT_VBLANK_INTERRUPT);
				break;
			case SPRITES:
				shouldFire = BitUtils.isBitSetted(stat, STAT_OAM_INTERRUPT);
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
		if (coincidence && BitUtils.isBitSetted(mmu.getByte(MMU.LCD_STATUS), STAT_LYC_INTERRUPT)) {
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
		return BitUtils.isBitSetted(getControlInfo(), LCDC_LCD_ENABLE);
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
		if (windowEnabled(control)) {
			renderWindow(control);
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

			boolean xFlip = BitUtils.isBitSetted(attributes, SPRITE_ATTR_X_FLIP);
			boolean yFlip = BitUtils.isBitSetted(attributes, SPRITE_ATTR_Y_FLIP);
			boolean bgPriority = BitUtils.isBitSetted(attributes, SPRITE_ATTR_BG_PRIORITY);
			boolean usePalette1 = BitUtils.isBitSetted(attributes, SPRITE_ATTR_PALETTE);

			int tileRow = resolveSpriteTileRow(line - spriteY, spriteHeight, yFlip);
			int[] adjusted = adjustTileIndexForDoubleHeight(tileIndex, tileRow, spriteHeight);
			tileIndex = adjusted[0];
			tileRow = adjusted[1];

			int tileDataAddress = TILE_DATA_UNSIGNED_BASE + tileIndex * BYTES_PER_TILE + tileRow * BYTES_PER_TILE_ROW;
			int byte1 = mmu.getByte(tileDataAddress);
			int byte2 = mmu.getByte(tileDataAddress + 1);

			for (int column = 0; column < TILE_WIDTH_PIXELS; column++) {
				int screenX = xPosition - SPRITE_X_OFFSET + column;
				if (isPixelOffScreen(screenX)) {
					continue;
				}

				int bit = xFlip ? column : (TILE_WIDTH_PIXELS - 1) - column;
				int colorIndex = decodeTileColorIndex(byte1, byte2, bit);
				renderSpritePixel(screenX, colorIndex, bgPriority, usePalette1);
			}
		}
	}

	int getSpriteHeight(int control) {
		return BitUtils.isBitSetted(control, LCDC_SPRITE_SIZE) ? 16 : 8;
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
			if (tileRow >= TILE_WIDTH_PIXELS) {
				return new int[]{tileIndex | 0x01, tileRow - TILE_WIDTH_PIXELS};
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
		return BitUtils.isBitSetted(control, LCDC_SPRITE_ENABLE);
	}

	private void renderBackground(int control) {
		int scrollY = mmu.getByte(MMU.LCD_SCROLL_Y);
		int scrollX = mmu.getByte(MMU.LCD_SCROLL_X);
		int tileDataBase = getTileDataBase(control);
		boolean useUnsigned = useUnsignedTileAddressing(control);
		int tileMapBase = getBackgroundTileMapBase(control);

		int posY = (scrollY + line) & 0xFF;
		int tileMapRow = (posY / TILE_WIDTH_PIXELS) * TILE_MAP_ROW_LENGTH;
		int tileLine = (posY % TILE_WIDTH_PIXELS) * BYTES_PER_TILE_ROW;
		for (int pixel = 0; pixel < SCREEN_WIDTH; pixel++) {
			int xPos = (pixel + scrollX) & 0xFF;
			int tileIndex = readTileIndex(tileMapBase, tileMapRow, xPos / TILE_WIDTH_PIXELS, useUnsigned);
			int tileDataAddress = resolveTileDataAddress(tileDataBase, useUnsigned, tileIndex);
			int byte1 = mmu.getByte(tileDataAddress + tileLine);
			int byte2 = mmu.getByte(tileDataAddress + tileLine + 1);
			int colorIndex = decodeTileColorIndex(byte1, byte2, (TILE_WIDTH_PIXELS - 1) - (xPos % TILE_WIDTH_PIXELS));
			renderBackgroundPixel(pixel, colorIndex);
		}
	}

	private boolean windowEnabled(int control) {
		return BitUtils.isBitSetted(control, LCDC_WINDOW_ENABLE);
	}

	private void renderWindow(int control) {
		int windowY = mmu.getByte(MMU.WINDOW_Y);
		if (line < windowY) {
			return;
		}
		int startX = mmu.getByte(MMU.WINDOW_X) - WINDOW_X_OFFSET;
		if (startX > SCREEN_WIDTH - 1) {
			return;
		}

		int tileDataBase = getTileDataBase(control);
		boolean useUnsigned = useUnsignedTileAddressing(control);
		int tileMapBase = getWindowTileMapBase(control);

		int tileMapRow = (windowLineCounter / TILE_WIDTH_PIXELS) * TILE_MAP_ROW_LENGTH;
		int tileLine = (windowLineCounter % TILE_WIDTH_PIXELS) * BYTES_PER_TILE_ROW;
		int firstPixel = Math.max(0, startX);
		for (int pixel = firstPixel; pixel < SCREEN_WIDTH; pixel++) {
			int windowRelativeX = pixel - startX;
			int tileIndex = readTileIndex(tileMapBase, tileMapRow, windowRelativeX / TILE_WIDTH_PIXELS, useUnsigned);
			int tileDataAddress = resolveTileDataAddress(tileDataBase, useUnsigned, tileIndex);
			int byte1 = mmu.getByte(tileDataAddress + tileLine);
			int byte2 = mmu.getByte(tileDataAddress + tileLine + 1);
			int colorIndex = decodeTileColorIndex(byte1, byte2, (TILE_WIDTH_PIXELS - 1) - (windowRelativeX % TILE_WIDTH_PIXELS));
			renderBackgroundPixel(pixel, colorIndex);
		}
		windowLineCounter++;
	}

	int getTileDataBase(int control) {
		return BitUtils.isBitSetted(control, LCDC_TILE_DATA) ? TILE_DATA_UNSIGNED_BASE : TILE_DATA_SIGNED_BASE;
	}

	boolean useUnsignedTileAddressing(int control) {
		return BitUtils.isBitSetted(control, LCDC_TILE_DATA);
	}

	int getBackgroundTileMapBase(int control) {
		return BitUtils.isBitSetted(control, LCDC_BG_TILE_MAP) ? TILE_MAP_HIGH : TILE_MAP_LOW;
	}

	int getWindowTileMapBase(int control) {
		return BitUtils.isBitSetted(control, LCDC_WINDOW_TILE_MAP) ? TILE_MAP_HIGH : TILE_MAP_LOW;
	}

	int readTileIndex(int tileMapBase, int tileMapRow, int tileColumn, boolean useUnsigned) {
		int tileAddress = tileMapBase + tileMapRow + tileColumn;
		return useUnsigned ? mmu.getByte(tileAddress) : mmu.getByteSigned(tileAddress);
	}

	int resolveTileDataAddress(int tileDataBase, boolean useUnsigned, int tileIndex) {
		return tileDataBase + (tileIndex + (useUnsigned ? 0 : SIGNED_TILE_INDEX_OFFSET)) * BYTES_PER_TILE;
	}

	void renderBackgroundPixel(int pixel, int colorIndex) {
		backgroundColorIndices[pixel] = colorIndex;
		int bgp = mmu.getByte(MMU.PALETTE_BGP);
		int shade = (bgp >> (colorIndex * 2)) & 0x03;
		frameBuffer.setPixel(pixel, line, shade);
	}

	private boolean backgroundEnabled(int control) {
		return BitUtils.isBitSetted(control, LCDC_BG_ENABLE);
	}

}
