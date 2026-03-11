package com.almejo.osom

import com.almejo.osom.gpu.FrameBuffer
import groovy.util.logging.Slf4j

/**
 * Headless Tetris runner for opcode discovery.
 * Run this to find which opcodes are missing when running Tetris.
 * Not a test — a discovery tool invoked manually.
 */
@Slf4j
class HeadlessTetrisRunner {

	static void main(String[] args) {
		if (args.length == 0) {
			log.error("Usage: HeadlessTetrisRunner <rom-path> [target-frames]")
			log.error("Example: HeadlessTetrisRunner roms/tetris.gb 1800")
			System.exit(1)
			return
		}

		String romPath = args[0]
		int targetFrames = args.length > 1 ? Integer.parseInt(args[1]) : 1800

		log.info("=== Headless Tetris Runner ===")
		log.info("ROM: {}", romPath)
		log.info("Target frames: {}", targetFrames)

		FrameBuffer frameBuffer = new FrameBuffer()
		Emulator emulator = new Emulator()
		emulator.initialize(false, romPath, frameBuffer)

		int framesCompleted = 0
		try {
			for (int frame = 0; frame < targetFrames; frame++) {
				emulator.runFrame()
				framesCompleted++
				if (framesCompleted % 100 == 0) {
					log.info("f:{} cycles:{}", framesCompleted, emulator.totalCycles)
				}
			}
			log.info("=== SUCCESS: {}/{} frames completed ===", framesCompleted, targetFrames)
		} catch (RuntimeException exception) {
			log.info("=== STOPPED at frame {} ===", framesCompleted)
			log.info("Exception: {}", exception.message)
		}
	}
}
