package com.almejo.osom.gpu

import spock.lang.Specification

class FrameBufferSpec extends Specification {

	def "setPixel and getPixels store and retrieve pixel values correctly"() {
		given:
		def frameBuffer = new FrameBuffer()

		when:
		frameBuffer.setPixel(10, 20, 3)

		then:
		frameBuffer.getPixels()[10][20] == 3
	}

	def "pixels array has correct dimensions"() {
		given:
		def frameBuffer = new FrameBuffer()

		expect:
		frameBuffer.getPixels().length == FrameBuffer.WIDTH
		frameBuffer.getPixels()[0].length == FrameBuffer.HEIGHT
	}

	def "pixels default to zero"() {
		given:
		def frameBuffer = new FrameBuffer()

		expect:
		frameBuffer.getPixels()[0][0] == 0
		frameBuffer.getPixels()[159][143] == 0
	}

	def "each FrameBuffer instance has independent pixel storage"() {
		given:
		def frameBuffer1 = new FrameBuffer()
		def frameBuffer2 = new FrameBuffer()

		when:
		frameBuffer1.setPixel(5, 5, 2)

		then:
		frameBuffer1.getPixels()[5][5] == 2
		frameBuffer2.getPixels()[5][5] == 0
	}
}
