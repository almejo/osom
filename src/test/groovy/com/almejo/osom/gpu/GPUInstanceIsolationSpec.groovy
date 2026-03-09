package com.almejo.osom.gpu

import spock.lang.Specification

class GPUInstanceIsolationSpec extends Specification {

    def "two GPU instances have independent pixel arrays"() {
        given: "two separate GPU instances"
        def gpu1 = new GPU()
        def gpu2 = new GPU()

        when: "we access both pixel arrays"
        def pixels1 = gpu1.getPixels()
        def pixels2 = gpu2.getPixels()

        then: "they are distinct objects"
        !pixels1.is(pixels2)
    }
}
