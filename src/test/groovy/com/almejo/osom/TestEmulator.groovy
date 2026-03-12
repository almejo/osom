package com.almejo.osom

import com.almejo.osom.gpu.FrameBuffer
import com.almejo.osom.input.Joypad

import java.util.zip.CRC32

class TestEmulator {

	final FrameBuffer frameBuffer
	final Emulator emulator

	TestEmulator(String romPath) {
		this(romPath, false)
	}

	TestEmulator(String romPath, boolean bootBios) {
		frameBuffer = new FrameBuffer()
		Joypad joypad = new Joypad()
		emulator = new Emulator()
		emulator.initialize(bootBios, romPath, frameBuffer, joypad)
	}

	void runFrames(int count) {
		for (int frame = 0; frame < count; frame++) {
			emulator.runFrame()
		}
	}

	long computeFramebufferChecksum() {
		def checksum = new CRC32()
		for (int x = 0; x < FrameBuffer.WIDTH; x++) {
			for (int y = 0; y < FrameBuffer.HEIGHT; y++) {
				checksum.update(frameBuffer.pixels[x][y])
			}
		}
		return checksum.getValue()
	}
}
