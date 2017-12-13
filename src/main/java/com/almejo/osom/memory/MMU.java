package com.almejo.osom.memory;

public class MMU {

	private Integer [] memory = new Integer[0x10000];
	private Cartridge cartridge;

	public void addCartridge(Cartridge cartridge) {
		this.cartridge = cartridge;
	}

	public void setByte(int address, int value) {
		memory[address] = value & 0x00ff;
	}

	public int getByte(int address) {
		if (address >=0 && address <= 0x3fff) {
			return this.cartridge.getByte(address);
		}
		return memory[address];
	}
}
