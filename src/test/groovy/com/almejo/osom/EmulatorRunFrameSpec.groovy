package com.almejo.osom

import com.almejo.osom.cpu.Z80Cpu
import com.almejo.osom.gpu.FrameBuffer
import com.almejo.osom.gpu.GPU
import com.almejo.osom.memory.Cartridge
import com.almejo.osom.memory.MMU
import spock.lang.Specification

class EmulatorRunFrameSpec extends Specification {

	private static final int ROM_SIZE = 0x8000

	private MMU mmu
	private Z80Cpu cpu
	private GPU gpu
	private Emulator emulator

	def setup() {
		mmu = new MMU(false)
		byte[] nopRom = new byte[ROM_SIZE]
		Cartridge cartridge = new Cartridge(nopRom)
		mmu.addCartridge(cartridge)

		cpu = new Z80Cpu(mmu, 4194304)
		mmu.setCpu(cpu)
		cpu.reset(false)

		gpu = new GPU()
		gpu.setMmu(mmu)
		gpu.setFrameBuffer(new FrameBuffer())
		// Allow OAM/VRAM test data writes (cpu.reset enables LCD, gpu.setMmu sets SPRITES mode)
		mmu.setStatModeBits(GPU.H_BLANK)

		emulator = new Emulator(cpu, gpu, mmu)
	}

	def "runFrame calls updateDma and completes DMA within one frame"() {
		given: "source data in work RAM and DMA triggered"
		mmu.setByte(0xC000, 0x42)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		and: "DMA is active"
		assert mmu.isDmaActive()

		when: "running one frame (69905 cycles >> 640 DMA cycles)"
		emulator.runFrame()

		then: "DMA has completed"
		!mmu.isDmaActive()

		and: "data was copied to OAM"
		mmu.getByte(0xFE00) == 0x42
	}

	def "runFrame accumulates totalCycles"() {
		given: "emulator starts with zero cycles"
		assert emulator.totalCycles == 0

		when: "running one frame"
		emulator.runFrame()

		then: "totalCycles is approximately one frame worth of cycles"
		emulator.totalCycles >= 69905
	}

	def "runFrame executes CPU, DMA, timers, interrupts, and GPU in sequence"() {
		given: "values in HRAM and work RAM"
		mmu.setByte(0xFF80, 0xAA)
		mmu.setByte(0xC100, 0xBB)

		and: "DMA is active"
		mmu.setByte(0xC000, 0x01)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		when: "running one frame"
		emulator.runFrame()

		then: "DMA completed and memory access is fully restored"
		!mmu.isDmaActive()
		mmu.getByte(0xFF80) == 0xAA
		mmu.getByte(0xC100) == 0xBB
	}

	def "runFrame with no active DMA runs normally"() {
		given: "no DMA active"
		assert !mmu.isDmaActive()

		when: "running one frame"
		emulator.runFrame()

		then: "frame completes without error"
		emulator.totalCycles >= 69905
	}

	def "DMA restriction is enforced during frame execution"() {
		given: "write a value to work RAM, then trigger DMA"
		mmu.setByte(0xC500, 0x77)
		mmu.setByte(0xC000, 0x01)
		mmu.setByte(MMU.DMA_ADDRESS, 0xC0)

		and: "DMA is active — reads outside HRAM should return 0xFF"
		assert mmu.isDmaActive()
		assert mmu.getByte(0xC500) == 0xFF

		when: "running one frame completes DMA"
		emulator.runFrame()

		then: "full memory access restored — work RAM readable again"
		mmu.getByte(0xC500) == 0x77
	}

	def "runFrame calls subsystems in correct order and equal count per iteration"() {
		given: "mocks with a simulated clock that advances 4 T-cycles per execute"
		List<String> callOrder = []
		int clockValue = 0

		Z80Cpu cpu = Mock(Z80Cpu)
		MMU mmu = Mock(MMU)
		GPU gpu = Mock(GPU)

		cpu.getClockT() >> { clockValue }
		cpu.execute() >> { clockValue += 4; callOrder << 'execute' }
		mmu.updateDma(_ as int) >> { callOrder << 'updateDma' }
		cpu.updateTimers(_ as int) >> { callOrder << 'updateTimers' }
		cpu.checkInterrupts() >> { callOrder << 'checkInterrupts' }
		gpu.update(_ as int) >> { callOrder << 'gpu.update' }

		Emulator emulator = new Emulator(cpu, gpu, mmu)

		when: "running one frame"
		emulator.runFrame()

		then: "each subsystem called at least 17000 times (one NOP frame at 4 T-cycles each)"
		callOrder.count('execute') >= 17000
		callOrder.count('updateDma') >= 17000
		callOrder.count('updateTimers') >= 17000
		callOrder.count('checkInterrupts') >= 17000
		callOrder.count('gpu.update') >= 17000

		and: "all subsystems called the exact same number of times"
		callOrder.count('execute') == callOrder.count('updateDma')
		callOrder.count('updateDma') == callOrder.count('updateTimers')
		callOrder.count('updateTimers') == callOrder.count('checkInterrupts')
		callOrder.count('checkInterrupts') == callOrder.count('gpu.update')

		and: "calls repeat in order: execute → updateDma → updateTimers → checkInterrupts → gpu.update"
		List<String> expectedPattern = ['execute', 'updateDma', 'updateTimers', 'checkInterrupts', 'gpu.update']
		callOrder.size() % 5 == 0
		for (int i = 0; i + 4 < callOrder.size(); i += 5) {
			assert callOrder[i..(i + 4)] == expectedPattern
		}
	}
}
