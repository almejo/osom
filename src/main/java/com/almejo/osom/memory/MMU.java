package com.almejo.osom.memory;

import com.almejo.osom.cpu.BitUtils;
import com.almejo.osom.cpu.Z80Cpu;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class MMU {
	private static final int DIVIDER_REGISTER_ADDRESS = 0xFF04;
	public static final int INTERRUPT_CONTROLLER_ADDRESS = 0xFF0F;
	public static final int INTERRUPT_ENABLED_ADDRESS = 0xFFFF;

	public static final int INTERRUPT_VBLANK = 0;
	public static final int INTERRUPT_LCD_STAT = 1;
	public static final int INTERRUPT_TIMER = 2;
	public static final int INTERRUPT_SERIAL = 3;
	public static final int INTERRUPT_JOYPAD = 4;
	public static final int DMA_ADDRESS = 0xFF46;
	private static final int IO_REGISTER = 0xFF00;

	public static final int TIMER_ADDRESS = 0xFF05;
	public static final int TIMER_MODULATOR = 0xFF06;
	public static final int TIMER_CONTROLLER = 0xFF07;

	public static final int LCD_CONTROLLER = 0xFF40;
	public static final int LCD_STATUS = 0xFF41;
	public static final int LCD_SCROLL_Y = 0xFF42;
	public static final int LCD_SCROLL_X = 0xFF43;
	public static final int LCD_LINE_COUNTER = 0xFF44;
	public static final int LCD_LY_COMPARE = 0xFF45;

	private boolean useBios;
	private final int[] ram = new int[0xffff + 1];
	private final int[] external = new int[0x1fff + 1];
	private final int[] sprites = new int[0x9F + 1];
	private final int[] bios;
	private Cartridge cartridge;
	private Z80Cpu cpu;

	public MMU(boolean useBios) throws IOException {
		this.useBios = useBios;
		if (useBios) {
			bios = ByteUtils.getBytes(Files.readAllBytes(Paths.get("bios/bios.bin")));
		} else {
			bios = new int[0];
		}
	}

	public void addCartridge(Cartridge cartridge) {
		this.cartridge = cartridge;
	}

	public void setByte(int address, int value) {
		value &= 0x00ff;
		if (address >= 0x8000 && address <= 0x9fff) {
			ram[address] = value;
		} else if (address >= 0xA000 && address <= 0xBFFF) {
			external[address - 0xa000] = value;
		} else if (address >= 0xFE00 && address <= 0xFE9F) {
			sprites[address - 0xFE00] = value;
		} else if (address == DMA_ADDRESS) {
			doDMATransfer(value);
		} else if (address == LCD_LINE_COUNTER) {
			ram[address] = 0;
		} else if (address == LCD_CONTROLLER) {
			ram[address] = value;
		} else if (address == LCD_STATUS) {
			// Only write bits 3-6 (interrupt enables); bits 0-2 are read-only (mode + coincidence flag)
			ram[LCD_STATUS] = (value & 0x78) | (ram[LCD_STATUS] & 0x07);
		} else if (address == LCD_SCROLL_Y || address == LCD_SCROLL_X || address == LCD_LY_COMPARE) {
			ram[address] = value;
		} else if (address == INTERRUPT_CONTROLLER_ADDRESS) {
			ram[address] = value;
		} else if (address >= 0xFF80 && address <= 0xFFFF) {
			ram[address] = value;
		} else if (address == IO_REGISTER) {
			ram[IO_REGISTER] = value;
		} else if (address == TIMER_ADDRESS || address == TIMER_MODULATOR) {
			ram[address] = value;
		} else if (address == TIMER_CONTROLLER) {
			updateTimerFrequency(value);
		} else if (address == DIVIDER_REGISTER_ADDRESS) {
			ram[DIVIDER_REGISTER_ADDRESS] = 0;
		} else if (address >= 0x0000 && address <= 0xDFFF) {
			ram[address] = value;
		} else if (address >= 0xE000 && address <= 0xFDFF) {
			ram[address - 0x2000] = value;
		} else {
			log.warn("Unhandled write: address=0x{} value=0x{}", String.format("%04X", address), String.format("%02X", value));
		}
	}

	private void doDMATransfer(int value) {
		int address = value << 8; // source address is data * 100
		for (int i = 0; i < 0xA0; i++) {
			setByte(0xFE00 + i, getByte(address + i));
		}
	}

	private void updateTimerFrequency(int value) {
		int oldFrequency = getTimerFrequency();
		ram[TIMER_CONTROLLER] = value;
		if (oldFrequency != (value & 0x03)) {
			this.cpu.updateTimerCounter(value & 0x03);
		}
	}

	private int getTimerFrequency() {
		return ram[TIMER_CONTROLLER] & 3;
	}

	public int getByte(int address) {
		if (address >= 0 && address <= 0x7fff) {
			if (useBios && address <= 0x100) {
				if (address == 0x100) {
					useBios = false;
					return this.cartridge.getByte(address);
				}
				return bios[address];
			}
			return this.cartridge.getByte(address);
		} else if (address >= 0x8000 && address <= 0x9fff) {
			return ram[address];
		} else if (address >= 0xA000 && address <= 0xBFFF) {
			return external[address - 0xa000];
		} else if (address >= 0xFE00 && address <= 0xFE9F) {
			return sprites[address - 0xFE00];
		} else if (address >= 0xFEA0 && address <= 0xFEFF) {
			return 0;
		} else if (address == IO_REGISTER) {
			return getIOState();
		} else if (address == LCD_STATUS) {
			// Bit 7 always reads as 1 on real hardware
			return ram[LCD_STATUS] | 0x80;
		} else if (address == LCD_LINE_COUNTER
				|| address == LCD_CONTROLLER
				|| address == LCD_SCROLL_Y
				|| address == LCD_SCROLL_X
				|| address == LCD_LY_COMPARE
				|| address == INTERRUPT_CONTROLLER_ADDRESS
				|| address == DIVIDER_REGISTER_ADDRESS
				|| address == TIMER_ADDRESS
				|| address == TIMER_MODULATOR
				|| address == TIMER_CONTROLLER) {
			return ram[address];
		} else if (address > 0xFF00 && address <= 0xFF7F) {
			log.warn("Unhandled read: address=0x{} returning 0", String.format("%04X", address));
			return 0;
		} else if (address >= 0xFF80 && address <= 0xFFFF) {
			return ram[address];
		} else if (address >= 0xC000 && address <= 0xDFFF) {
			return ram[address];
		} else if (address >= 0xE000 && address <= 0xFDFF) {
			return ram[address - 0x2000];
		}
		throw new UnreadableMemoryLocation(address);
	}

	private int getIOState() {
		return 0xDF;
	}

	public int getWord(int address) {
		return getByte(address + 1) << 8 | getByte(address);
	}

	public void setWord(int address, int word) {
		int hi = word >> 8;
		int lo = word & 0x00ff;
		setByte(address + 1, hi);
		setByte(address, lo);
	}

	public int getByteSigned(int address) {
		return BitUtils.toSignedByte(getByte(address));
	}

	public void setCpu(Z80Cpu cpu) {
		this.cpu = cpu;
	}

	public void incrementDividerRegister() {
		ram[DIVIDER_REGISTER_ADDRESS] += 1;
		if (ram[DIVIDER_REGISTER_ADDRESS] == 256) {
			ram[DIVIDER_REGISTER_ADDRESS] = 0;
		}
	}

	public void setScanline(int lineNumber) {
		ram[LCD_LINE_COUNTER] = lineNumber;
	}

	public void setStatModeBits(int mode) {
		ram[LCD_STATUS] = (ram[LCD_STATUS] & 0xFC) | (mode & 0x03);
	}

	public void setStatCoincidenceFlag(boolean coincidence) {
		if (coincidence) {
			ram[LCD_STATUS] = ram[LCD_STATUS] | 0x04;
		} else {
			ram[LCD_STATUS] = ram[LCD_STATUS] & ~0x04;
		}
	}

	public void requestInterrupt(int bit) {
		log.debug("Requested interrupt bit={}", bit);
		int value = getByte(INTERRUPT_CONTROLLER_ADDRESS);
		setByte(INTERRUPT_CONTROLLER_ADDRESS, BitUtils.setBit(value, bit));
	}

}
