package com.almejo.osom.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MMU {
	private boolean booting = true;
	private int[] memory = new int[0x10000];
	private int[] bios;
	private Cartridge cartridge;

	public MMU() throws IOException {
		bios = ByteUtils.getBytes(Files.readAllBytes(Paths.get("bios/bios.bin")));
	}

	public void addCartridge(Cartridge cartridge) {
		this.cartridge = cartridge;
	}

	public void setByte(int address, int value) {
		memory[address] = value & 0x00ff;
	}

	public int getByte(int address) {
		if (address >= 0 && address <= 0x7fff) {
			if (booting && address <= 0x100) {
				if (address == 0x100) {
					booting = false;
				}
				return bios[address];
			}
			return this.cartridge.getByte(address);
		}
		return memory[address];
	}

	public int getWord(int address) {
		return getByte(address + 1) << 8 | getByte(address);
	}
}
