package com.almejo.osom.gpu;

import com.almejo.osom.cpu.BitUtils;
import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.memory.MMU;
import lombok.Setter;

public class GPU {
	private int line = 1;
	private int clock = 0;
	@Setter
	private FrameBuffer frameBuffer;
	@Setter
    private Z80Cpu cpu;
	private MMU mmu;

	public GPU() {
	}

    public void setMmu(MMU mmu) {
		this.mmu = mmu;
		mmu.setScanline(line);
	}

	public void update(int cycles) {
		if (!isEnabled()) {
			return;
		}
		clock += cycles;

		if (clock >= 456) {
			clock = 0;

			if (line < 145) {
				drawLine();
				if (line == 144) {
					cpu.requestInterrupt(Z80Cpu.INTERRUPT_ADDRESS_V_BLANK);
				}
				line++;
				mmu.setScanline(line);
			} else if (line >= 145 && line < 154) {
				mmu.setScanline(0);
				line++;
			} else {
				line = 1;
				mmu.setScanline(line);
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
			int tileLocation = tilesData + (tileIndex + (useUnsignedIdentifier ? 0 : 128)) * 16;
			int tileLine = (line % 8) * 2;// 2 bytes
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
