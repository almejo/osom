package com.almejo.osom.cpu

import com.almejo.osom.memory.MMU
import spock.lang.Specification

class CpuInstanceIsolationSpec extends Specification {

    def "two Z80Cpu instances have independent timer counters"() {
        given: "two separate CPU instances with stubbed MMUs"
        def mmu1 = Stub(MMU)
        def mmu2 = Stub(MMU)
        def cpu1 = new Z80Cpu(mmu1, 4194304)
        def cpu2 = new Z80Cpu(mmu2, 4194304)

        when: "timer counter is updated on cpu1 only"
        cpu1.updateTimerCounter(1)

        then: "cpu1's timer counter has changed but cpu2 remains at the initial default"
        cpu1.@timerCounter == 4194304 / 262144 as int
        cpu2.@timerCounter == 1024
    }
}
