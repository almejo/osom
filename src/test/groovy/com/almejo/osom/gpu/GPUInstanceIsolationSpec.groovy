package com.almejo.osom.gpu

import spock.lang.Specification

class GPUInstanceIsolationSpec extends Specification {

    def "two GPU instances with separate FrameBuffers have independent pixel storage"() {
        given: "two separate GPU instances with their own FrameBuffers"
        def gpu1 = new GPU()
        def frameBuffer1 = new FrameBuffer()
        gpu1.setFrameBuffer(frameBuffer1)

        def gpu2 = new GPU()
        def frameBuffer2 = new FrameBuffer()
        gpu2.setFrameBuffer(frameBuffer2)

        when: "we modify one FrameBuffer"
        frameBuffer1.setPixel(0, 0, 3)

        then: "the other FrameBuffer is unaffected"
        frameBuffer1.getPixels()[0][0] == 3
        frameBuffer2.getPixels()[0][0] == 0
    }
}
