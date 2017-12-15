package com.almejo.osom.memory;

public class MMU {

	private int[] memory = new int[0x10000];
	private Cartridge cartridge;

	public void addCartridge(Cartridge cartridge) {
		this.cartridge = cartridge;
	}

	public void setByte(int address, int value) {
		memory[address] = value & 0x00ff;
	}

	public int getByte(int address) {
		if (address >= 0 && address <= 0x7fff) {
			return this.cartridge.getByte(address);
		}
		return memory[address];
	}

	public int getWord(int address) {
		return getByte(address + 1) << 8 | getByte(address);
	}
}
