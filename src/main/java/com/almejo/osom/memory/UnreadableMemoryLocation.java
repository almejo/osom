package com.almejo.osom.memory;

class UnreadableMemoryLocation extends RuntimeException {
	UnreadableMemoryLocation(int address) {
		super("0x" + Integer.toHexString(address));
	}
}
