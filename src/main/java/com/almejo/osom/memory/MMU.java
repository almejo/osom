package com.almejo.osom.memory;

import com.almejo.osom.cpu.Z80Cpu;
import com.almejo.osom.gpu.GPU;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MMU {
	private static final int DIVIDER_REGISTER_ADDRESS = 0xFF04;
	public static final int INTERRUPT_CONTROLLER_ADDRESS = 0xFF0F;
	public static final int INTERRUPT_ENABLED_ADDRESS = 0xFFFF;
	public static int TIMER_ADDRESS = 0xFF05;
	public static int TIMER_MODULATOR = 0xFF06;
	public static int TIMER_CONTROLLER = 0xFF07;

	private boolean useBios;
	private GPU gpu;
	private int[] ram = new int[0xffff];
	private int[] video = new int[0x1fff + 1];
	private int[] external = new int[0x1fff + 1];
	private int[] sprites = new int[0x9F + 1];
	private int[] io = new int[0x7f + 1];
	private int[] zero = new int[0x7f + 1];
	private int[] bios;
	private Cartridge cartridge;
	private Z80Cpu cpu;


	public void updatetile(int address) {
		// Get the "base address" for this tile row
		address &= 0x1FFE; // Work out which tile and row was updated var
		int tile = (address >> 4) & 0x1ff;
		int y = (address >> 1) & 7;

		int sx;
		for (int x = 0; x < 8; x++) {
			// Find bit index for this pixel
			sx = 1 << (7 - x);
			// Update tile set
			gpu.updateTile(tile, y, x, ((video[address] & sx) > 0 ? 1 : 0) + ((video[address + 1] & sx) > 0 ? 2 : 0));
		}
	}


	public MMU(boolean useBios, GPU gpu) throws IOException {
		this.useBios = useBios;
		this.gpu = gpu;
		bios = ByteUtils.getBytes(Files.readAllBytes(Paths.get("bios/bios.bin")));
	}

	public void addCartridge(Cartridge cartridge) {
		this.cartridge = cartridge;
	}

	public void setByte(int address, int value) {
		value &= 0x00ff;
//		if (address >= 0 && address <= 0x7fff) {
//			return;
//		} else
		if (address >= 0x8000 && address <= 0x9fff) {
			video[address - 0x8000] = value;
			updatetile(address - 0x8000);
		} else if (address >= 0xA000 && address <= 0xBFFF) {
			external[address - 0xa000] = value;
		} else if (address >= 0xFE00 && address <= 0xFE9F) {
			sprites[address - 0xFE00] = value;
//		} else if (address >= 0xFEA0 && address <= 0xFEFF) {
//			return;
//		} else if (address >= 0xFF00 && address <= 0xFF7F) {
//			return; // io[address - 0xFF00] = value;
		} else if (address >= 0xFF80 && address <= 0xFFFF) {
			zero[address - 0xFF80] = value;
		} else if (address == TIMER_CONTROLLER) {
			updateTimerFrecuency(value);
		} else if (address == DIVIDER_REGISTER_ADDRESS) {
			ram[DIVIDER_REGISTER_ADDRESS] = 0;
		} else if (address >= 0x0000 && address <= 0xDFFF) {
			ram[address] = value;
		} else if (address >= 0xE000 && address <= 0xFDFF) {
			ram[address - 0x2000] = value;
		}
	}

	private void updateTimerFrecuency(int value) {
		int oldFrequency = getTimerFrequency();
		if (oldFrequency != (value & 3)) {
			setFrequency(value);
		}
	}

	private void setFrequency(int value) {
		ram[TIMER_ADDRESS] = value;
		this.cpu.updateTimerCounter(value);
	}

	private int getTimerFrequency() {
		return ram[TIMER_ADDRESS] & 3;
	}

	public int getByte(int address) {
		if (address >= 0 && address <= 0x7fff) {
			if (useBios && address <= 0x100) {
				if (address == 0x100) {
					useBios = false;
				}
				return bios[address];
			}
			return this.cartridge.getByte(address);
		} else if (address >= 0x8000 && address <= 0x9fff) {
			return video[address - 0x8000];
		} else if (address >= 0xA000 && address <= 0xBFFF) {
			return external[address - 0xa000];
		} else if (address >= 0xFE00 && address <= 0xFE9F) {
			return sprites[address - 0xFE00];
		} else if (address >= 0xFEA0 && address <= 0xFEFF) {
			return 0;
		} else if (address >= 0xFF00 && address <= 0xFF7F) {
			return 0; // io[address - 0xFF00];
		} else if (address >= 0xFF80 && address <= 0xFFFF) {
			return zero[address - 0xFF80];
		} else if (address >= 0xC000 && address <= 0xDFFF) {
			return ram[address];
		} else if (address >= 0xE000 && address <= 0xFDFF) {
			return ram[address - 0x2000];
		}
		throw new UnreadableMemoryLocation(address);
	}

	public int getWord(int address) {
		return getByte(address + 1) << 8 | getByte(address);
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

}
