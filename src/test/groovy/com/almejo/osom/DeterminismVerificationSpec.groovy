package com.almejo.osom

import com.almejo.osom.gpu.FrameBuffer
import spock.lang.Requires
import spock.lang.Specification

@Requires({ new File('roms/tetris.gb').exists() })
class DeterminismVerificationSpec extends Specification {

	private static final String TETRIS_ROM_PATH = 'roms/tetris.gb'
	private static final int FRAME_COUNT = 300

	def "Tetris produces identical framebuffer checksums across two runs"() {
		given:
		def firstEmulator = new TestEmulator(TETRIS_ROM_PATH)
		def secondEmulator = new TestEmulator(TETRIS_ROM_PATH)

		when:
		firstEmulator.runFrames(FRAME_COUNT)
		secondEmulator.runFrames(FRAME_COUNT)

		then:
		def firstChecksum = firstEmulator.computeFramebufferChecksum()
		def secondChecksum = secondEmulator.computeFramebufferChecksum()

		firstChecksum == secondChecksum
		hasNonZeroPixels(firstEmulator.frameBuffer)
		hasNonZeroPixels(secondEmulator.frameBuffer)
		firstEmulator.emulator.totalCycles == secondEmulator.emulator.totalCycles
	}

	private boolean hasNonZeroPixels(FrameBuffer frameBuffer) {
		for (int x = 0; x < FrameBuffer.WIDTH; x++) {
			for (int y = 0; y < FrameBuffer.HEIGHT; y++) {
				if (frameBuffer.pixels[x][y] != 0) {
					return true
				}
			}
		}
		return false
	}
}
