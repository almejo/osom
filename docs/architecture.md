# OSOM Architecture

This document describes the architecture of OSOM, a learning-first Game Boy emulator written in Java. It covers the main emulation loop, component responsibilities, data flow, and how subsystems communicate.

## Overview

OSOM emulates the Nintendo Game Boy's LR35902 CPU (a Z80 variant), GPU, memory subsystem, and timer hardware. The emulator runs Game Boy ROMs at 60 frames per second, executing approximately 4,194,304 CPU T-cycles per second (the real Game Boy's clock speed).

The project prioritizes clarity and learning over performance. Each CPU instruction is its own class, memory access routes through a central MMU, and the GPU renders scanlines to a pixel buffer that the display reads independently.

## Architecture Layers

OSOM is split into two layers: the **emulation core** and the **presentation layer**.

```
src/main/java/com/almejo/osom/
    Emulator.java          (core orchestrator)
    Main.java              (CLI entry point)
    cpu/                   (core: CPU, ALU, opcodes, registers)
    memory/                (core: MMU, Cartridge, address routing)
    gpu/                   (core: GPU rendering, FrameBuffer)
    ui/                    (presentation: Swing window, LCD display)
```

**Dependency direction:** The `ui/` package imports from core packages (`cpu/`, `memory/`, `gpu/`, and `Emulator`). Core packages never import from `ui/`. This is enforced by a structural test (`CorePresentationSeparationSpec`) that scans source files for `javax.swing` and `java.awt` imports in core packages on every build.

The `FrameBuffer` class in `gpu/` is the bridge between the two layers. The GPU writes pixels to it; the `LCDScreen` in `ui/` reads pixels from it. Neither side knows about the other.

## Component Responsibilities

### Z80Cpu (`cpu/Z80Cpu.java`)

Emulates the Game Boy's LR35902 CPU. Manages:

- **Registers:** AF, BC, DE, HL (general-purpose 16-bit pairs), PC (program counter), SP (stack pointer). Each is a `Register` object with hi/lo byte access.
- **Flags:** Stored in the low byte of AF. Bit positions: Zero (bit 7), Subtract (bit 6), Half-Carry (bit 5), Carry (bit 4).
- **Opcode dispatch:** Two `HashMap<Integer, Operation>` maps — `operations` for standard opcodes and `operationsCB` for CB-prefixed opcodes. The constructor registers ~95 opcodes with collision detection that throws `IllegalStateException` on duplicates.
- **Timers:** `updateTimers(cycles)` advances the divider register (0xFF04, increments when cycle accumulator reaches 255) and the TIMA timer counter (0xFF05, frequency-configurable via TAC register at 0xFF07).
- **Interrupts:** `requestInterrupt(int bit)` sets a bit in the IF register (0xFF0F). `checkInterrupts()` iterates bits 0-4 of IF, checks against IE (0xFFFF), and dispatches to the highest-priority pending interrupt by pushing PC onto the stack and jumping to the interrupt service routine address.

Interrupt priorities and ISR addresses (bit 0 is highest priority):

| Bit | Source   | ISR Address |
|-----|----------|-------------|
| 0   | V-Blank  | 0x0040      |
| 1   | LCD STAT | 0x0048      |
| 2   | Timer    | 0x0050      |
| 3   | Serial   | 0x0058      |
| 4   | Joypad   | 0x0060      |

### Operation (`cpu/Operation.java`)

Abstract base class for all CPU instructions. Each opcode is a subclass with a naming convention: `Operation{MNEMONIC}_{OPERANDS}` (e.g., `OperationLD_A_n`, `OperationXOR_A`, `OperationBIT_7_H`).

Each operation defines:
- `code` — the opcode byte value
- `length` — instruction byte length (for PC advancement)
- `m` and `t` — M-cycles and T-cycles consumed
- `execute()` — the instruction logic

Parameterized base classes reduce duplication. For example, `OperationLD_r_n` takes a register argument so all `LD r, n` instructions share logic. CB-prefixed opcodes extend `OperationCB` (which extends `Operation`), and are routed to the `operationsCB` dispatch map automatically.

There are ~122 Operation source files, including abstract base classes and parameterized variants.

### ALU (`cpu/ALU.java`)

Arithmetic/Logic Unit. Provides flag-aware operations: `xor`, `or`, `addRegisterHI`, `subHI`, `dec`, `incLO`, `incHI`, `rotateLeft`, and comparison (`cpHI`). Each method updates the CPU flags (Zero, Subtract, Half-Carry, Carry) according to Game Boy rules.

### MMU (`memory/MMU.java`)

Memory Management Unit. Routes all memory reads (`getByte`, `getWord`) and writes (`setByte`, `setWord`) through a 16-bit address space.

Key features:
- **BIOS bootstrap:** When `useBios=true`, addresses 0x0000-0x00FF read from the 256-byte BIOS. At address 0x0100, the BIOS overlay is disabled and control passes to the cartridge ROM. When `useBios=false`, the BIOS array is empty and the cartridge is read directly.
- **DMA transfer:** A write to 0xFF46 triggers OAM DMA — copies 160 bytes from the source address (value << 8) to sprite memory (0xFE00-0xFE9F).
- **Timer frequency updates:** Writes to the TAC register (0xFF07) trigger `updateTimerFrequency()`, which recalculates the CPU's timer counter if the frequency changed.
- **Joypad placeholder:** `getIOState()` at 0xFF00 returns a hardcoded `0xDF` (all buttons released). Joypad input is not yet implemented.

### GPU (`gpu/GPU.java`)

Graphics processor. Implements scanline-based rendering driven by cycle accumulation.

- **Timing:** 456 T-cycles per scanline. The GPU uses 1-based line numbering (lines 1-154) rather than the 0-based convention (0-153) described in Pan Docs. Lines 1-144 are visible (background rendering). Line 144 triggers a V-Blank interrupt via `cpu.requestInterrupt()`. Lines 145-153 are the V-Blank period. After line 153, the GPU resets to line 1.
- **Background rendering:** `renderBackground()` reads the LCDC register (0xFF40) to determine tile data bank (0x8000 unsigned or 0x8800 signed) and tile map (0x9800 or 0x9C00). Applies scroll registers SCY (0xFF43) and SCX (0xFF42) for background offset. Writes pixel colors (0-3) to the FrameBuffer. The GPU reads tile data directly from VRAM via MMU on each scanline — there is no tile cache.
- **Sprites:** `renderSprites()` is an empty stub — sprite rendering is not yet implemented.
- **LCD enable:** The GPU checks bit 7 of LCDC. If the LCD is disabled, `update()` returns immediately.

### FrameBuffer (`gpu/FrameBuffer.java`)

A 160x144 pixel buffer that decouples the GPU from the display.

- `setPixel(int x, int y, int color)` — GPU writes pixels during rendering (color values 0-3)
- `getPixels()` — LCDScreen reads the full `int[160][144]` array for display

The FrameBuffer is created externally (by `EmulatorApp` or `TestEmulator`) and injected into both the GPU and LCDScreen. This enables headless operation — the GPU writes pixels regardless of whether anyone reads them.

### Emulator (`Emulator.java`)

Core orchestrator with zero Swing imports. Can be used headlessly.

- `initialize(boolean bootBios, String file, FrameBuffer frameBuffer)` — Creates and wires all core components: GPU, MMU, Cartridge, Z80Cpu. Sets the FrameBuffer on the GPU. Resets CPU state (PC to 0x0000 with BIOS or 0x0100 without).
- `runFrame()` — Executes one frame's worth of CPU cycles. This is the **inner emulation loop** (see below).
- `totalCycles` — Accumulates the total T-cycles executed across all frames.

### EmulatorApp (`ui/EmulatorApp.java`)

Swing application shell. Owns the **outer frame loop**, window creation, and frame throttling.

- Creates a JFrame at 2x scale (320x288 pixels), FrameBuffer, Emulator, and LCDScreen.
- Outer loop: calls `emulator.runFrame()`, updates debug stats on LCDScreen, calls `lcdScreen.repaint()`, logs FPS every second, and sleeps to throttle to ~60 FPS (16ms per frame).
- Graceful shutdown via `volatile boolean running` and a `WindowAdapter` on window close.

### LCDScreen (`ui/LCDScreen.java`)

Swing `JPanel` that reads the FrameBuffer and paints pixels at 2x scale.

- Iterates `frameBuffer.getPixels()` and draws each pixel as a 2x2 filled rectangle.
- Uses placeholder colors: 0=black, 1=green, 2=red, 3=blue (not the authentic Game Boy palette — palette implementation is planned for a future story).
- Renders a debug overlay with timestamp, cycle count, frame count, and elapsed time.
- Presentation-only — does not participate in emulation logic.

## Main Emulation Loop

The emulation runs as two nested loops:

```
EmulatorApp outer loop (ui/, owns frame throttling):
    while (running):
        startTime = currentTimeMillis()
        emulator.runFrame()                   // execute one frame
        delta = currentTimeMillis() - startTime
        lcdScreen.setCycles/setFrameCounter   // update debug overlay
        lcdScreen.repaint()                   // trigger display refresh
        log FPS every 1000ms
        Thread.sleep(16 - delta)              // throttle to ~60 FPS

Emulator.runFrame() inner loop (core, no Swing):
    cyclesToScreen = CYCLES_PER_FRAME         // 69,905 (4,194,304 / 60)
    while (cyclesToScreen > 0):
        oldCycles = cpu.clock.getT()
        cpu.execute()                         // fetch + dispatch + execute opcode
        cycles = cpu.clock.getT() - oldCycles // T-cycles consumed by this instruction
        totalCycles += cycles
        cpu.updateTimers(cycles)              // advance divider and TIMA
        cpu.checkInterrupts()                 // serve pending interrupts
        gpu.update(cycles)                    // advance scanline state machine
        cyclesToScreen -= cycles
```

The inner loop is purely cycle-driven — no wall-clock time, no random state. This makes the emulation deterministic: given identical initial state and ROM input, two runs produce identical output. The outer loop adds frame throttling and display refresh, which are presentation concerns only.

## Data Flow — One Frame

This is the complete flow for rendering one frame, from `EmulatorApp.loop()` through CPU/GPU/timers to `LCDScreen.repaint()`:

```
EmulatorApp
    |
    v
Emulator.runFrame()
    |
    |  (repeats ~69,905 / cycles-per-instruction times)
    |
    +---> Z80Cpu.execute()
    |         |
    |         +---> MMU.getByte(PC)       // fetch opcode
    |         +---> operations.get(code)  // dispatch
    |         +---> operation.execute()   // execute (may read/write via MMU)
    |         +---> clock.update(m, t)    // advance cycle counters
    |
    +---> Z80Cpu.updateTimers(cycles)
    |         |
    |         +---> dividerCounter += cycles
    |         |     if >= 255: mmu.incrementDividerRegister()
    |         |
    |         +---> timerCounter -= cycles
    |               if < 0 and timer overflow:
    |                   requestInterrupt(INTERRUPT_BIT_TIMER)
    |
    +---> Z80Cpu.checkInterrupts()
    |         |
    |         +---> read IF (0xFF0F) and IE (0xFFFF)
    |         +---> for bits 0-4: if set in both, serve interrupt
    |               (push PC, jump to ISR address, disable interrupts)
    |
    +---> GPU.update(cycles)
              |
              +---> clock += cycles
              +---> if clock >= 456 (one scanline):
                        if line < 145: renderBackground()
                            +---> read LCDC, SCX, SCY from MMU
                            +---> read tile data and tile map from VRAM
                            +---> frameBuffer.setPixel(x, line, color)
                        if line == 144: cpu.requestInterrupt(V-Blank)
                        advance line (wraps at 154 back to 1)

After runFrame() returns:
    EmulatorApp ---> lcdScreen.repaint()
                         |
                         +---> frameBuffer.getPixels()
                         +---> paint each pixel at 2x scale via Graphics2D
```

## Subsystem Communication Map

| From | To | Mechanism | Direction |
|------|----|-----------|-----------|
| EmulatorApp | Emulator | `emulator.runFrame()` | ui/ → core |
| Emulator | Z80Cpu | `cpu.execute()`, `updateTimers()`, `checkInterrupts()` | core → core |
| Emulator | GPU | `gpu.update(cycles)` | core → core |
| GPU | FrameBuffer | `frameBuffer.setPixel(x, y, color)` | core → core |
| GPU | Z80Cpu | `cpu.requestInterrupt(...)` | core → core |
| GPU | MMU | `mmu.getByte()` for LCDC, VRAM, scroll registers | core → core |
| Z80Cpu | MMU | `mmu.getByte()`, `mmu.setByte()` for all memory access | core → core |
| Z80Cpu (timer) | Z80Cpu | `requestInterrupt(INTERRUPT_BIT_TIMER)` on timer overflow | core → core |
| MMU | Z80Cpu | `cpu.updateTimerCounter()` on TAC frequency change | core → core |
| GPU | MMU | `mmu.setScanline(line)` to update LCD line counter | core → core |
| LCDScreen | FrameBuffer | `frameBuffer.getPixels()` | ui/ → core |
| EmulatorApp | LCDScreen | `lcdScreen.repaint()` | ui/ → ui/ |

## Memory Map

The MMU maps the Game Boy's 16-bit address space (0x0000-0xFFFF):

| Address Range | Size | Region | Notes |
|---------------|------|--------|-------|
| 0x0000-0x00FF | 256 B | BIOS overlay | Active during boot only; switches to cartridge ROM at 0x0100 |
| 0x0000-0x7FFF | 32 KB | Cartridge ROM | Read-only; routed to `Cartridge.getByte()` |
| 0x8000-0x9FFF | 8 KB | VRAM | Tile data (0x8000-0x97FF) and tile maps (0x9800-0x9FFF) |
| 0xA000-0xBFFF | 8 KB | External RAM | Cartridge RAM; stored in `external[]` array |
| 0xC000-0xDFFF | 8 KB | Work RAM | General-purpose RAM |
| 0xE000-0xFDFF | ~8 KB | Echo RAM | Mirror of 0xC000-0xDDFF (reads/writes redirect to Work RAM) |
| 0xFE00-0xFE9F | 160 B | OAM | Sprite attribute memory; stored in `sprites[]` array |
| 0xFEA0-0xFEFF | 96 B | Unused | Returns 0 on read |
| 0xFF00 | 1 B | Joypad | I/O register; returns hardcoded 0xDF (placeholder) |
| 0xFF04 | 1 B | DIV | Divider register; incremented by timer, reset to 0 on write |
| 0xFF05-0xFF07 | 3 B | Timer | TIMA (counter), TMA (modulo), TAC (control) |
| 0xFF0F | 1 B | IF | Interrupt Flag — pending interrupt requests |
| 0xFF40 | 1 B | LCDC | LCD Control register |
| 0xFF42-0xFF43 | 2 B | SCY/SCX | Background scroll Y and X |
| 0xFF44 | 1 B | LY | LCD line counter; reset to 0 on write |
| 0xFF46 | 1 B | DMA | Writing triggers OAM DMA transfer |
| 0xFF80-0xFFFE | 127 B | High RAM | Fast RAM accessible during DMA. Note: write to 0xFF80 is silently discarded (known bug) |
| 0xFFFF | 1 B | IE | Interrupt Enable — which interrupts are active |

## CPU Instruction Architecture

Each Z80 instruction is a dedicated Java class extending `Operation`. This one-class-per-opcode pattern prioritizes clarity over compactness:

```
Operation (abstract)
    |
    +-- OperationNOOP          (0x00, NOP)
    +-- OperationLD_A_n        (0x3E, LD A, n)
    +-- OperationXOR_A         (0xAF, XOR A)
    +-- OperationJP_nn         (0xC3, JP nn)
    +-- OperationCALL_nn       (0xCD, CALL nn)
    +-- ...
    |
    +-- OperationCB (abstract, CB-prefixed)
         +-- OperationBIT_7_H  (0xCB 0x7C, BIT 7, H)
         +-- OperationRL_C     (0xCB 0x11, RL C)
         +-- OperationSWAP_A   (0xCB 0x37, SWAP A)
         +-- OperationRES_0_A  (0xCB 0x87, RES 0, A)
         +-- ...
```

**Dispatch flow in `Z80Cpu.execute()`:**
1. Fetch the opcode byte at the current PC from memory via `mmu.getByte(PC)`
2. If opcode is 0xCB: advance PC, fetch next byte, look up in `operationsCB` map
3. Otherwise: look up in `operations` map
4. If not found: throw `RuntimeException` with full register state dump for diagnostics
5. Call `operation.execute()` to run the instruction logic
6. Call `operation.update(clock)` to advance M-cycle and T-cycle counters
7. If PC was not modified by the instruction (e.g., a jump), advance PC by `operation.length`

**Collision detection:** `addOpcode()` checks both dispatch maps before registering. If an opcode code is already registered, it throws `IllegalStateException` with the conflicting class names. This is enforced by structural tests on every build.

## Interrupt System

The Game Boy supports 5 interrupt sources, served in priority order (bit 0 highest):

**Registers:**
- **IF** (0xFF0F) — Interrupt Flag. Each bit indicates a pending interrupt request.
- **IE** (0xFFFF) — Interrupt Enable. Each bit enables/disables the corresponding interrupt.
- **IME** — Interrupt Master Enable. A CPU-internal flag (not memory-mapped) toggled by `DI` (disable) and `EI` (enable) instructions.

**Request flow:**
1. A subsystem calls `Z80Cpu.requestInterrupt(int bit)`, which sets the corresponding bit in the IF register via MMU.
2. The GPU requests V-Blank at scanline 144. The timer requests Timer interrupt on TIMA overflow. Note: the GPU currently passes `INTERRUPT_ADDRESS_V_BLANK` (0x40, an ISR address) instead of the bit index (0) to `requestInterrupt()` — this is a known pre-existing bug.
3. On each cycle batch, `checkInterrupts()` reads IF and IE. For each bit 0-4, if the bit is set in both IF and IE (and IME is enabled):
   - Clear the bit in IF
   - Disable IME (prevents nested interrupts)
   - Push current PC onto the stack
   - Set PC to the ISR address (looked up from `INTERRUPT_ADDRESSES` map)
4. The interrupt handler runs until a `RETI` instruction, which returns from the interrupt and re-enables IME.

## Timer Subsystem

The Game Boy has two timer mechanisms, both driven by CPU cycles:

**Divider Register (DIV, 0xFF04):**
- Incremented when the cycle accumulator reaches 255 via `updateDividerRegister()`
- Writing any value to 0xFF04 resets it to 0
- Free-running — cannot be stopped

**Timer Counter (TIMA/TMA/TAC):**
- **TIMA** (0xFF05) — Timer counter, increments at a frequency set by TAC
- **TMA** (0xFF06) — Timer modulo, loaded into TIMA on overflow
- **TAC** (0xFF07) — Timer control: bit 2 enables/disables, bits 0-1 select frequency

Timer frequencies (based on the 4,194,304 Hz clock):

| TAC bits 0-1 | Frequency | Cycles per tick |
|--------------|-----------|-----------------|
| 00 | 4,096 Hz | 1,024 |
| 01 | 262,144 Hz | 16 |
| 10 | 65,536 Hz | 64 |
| 11 | 16,384 Hz | 256 |

When TIMA reaches 255 and would overflow, it is reset to the value in TMA and a Timer interrupt is requested via `requestInterrupt(INTERRUPT_BIT_TIMER)`.

## Testing Architecture

OSOM uses three categories of tests, all running via Spock 2.4 on `useJUnitPlatform()`:

**Structural tests** (run on every build, no ROM needed):
- `OpcodeUniquenessSpec` — Verifies all standard and CB-prefixed opcodes have unique codes
- `CBPrefixSeparationSpec` — Verifies CB operations are only in the CB dispatch map
- `CorePresentationSeparationSpec` — Verifies no Swing/AWT imports in core packages

**Instance isolation tests** (run on every build, no ROM needed):
- `GPUInstanceIsolationSpec` — Verifies two GPU instances have independent pixel buffers
- `CpuInstanceIsolationSpec` — Verifies two Z80Cpu instances have independent timer counters
- `FrameBufferSpec` — Verifies pixel storage, dimensions, defaults, and instance isolation

**Integration / determinism tests** (conditional, require ROM):
- `DeterminismVerificationSpec` — Runs Tetris for 300 frames twice, compares CRC32 checksums of both framebuffers. Proves deterministic execution. Skips gracefully via `@Requires` when ROM is absent.
- `TestEmulator` — A helper class (not a Spec, not auto-discovered) that instantiates core components headlessly. Provides `runFrames(count)` and `computeFramebufferChecksum()` using `java.util.zip.CRC32`.

## Object Creation Graph

```
Main
 +-- parses CLI args (--rom, --no-bios, --log-level)
 +-- creates EmulatorApp
      |
      +-- creates FrameBuffer
      +-- creates Emulator
      |    +-- initialize():
      |         +-- creates GPU
      |         +-- creates MMU(useBios)
      |         +-- creates Cartridge(romBytes)
      |         +-- creates Z80Cpu(mmu, cycles)
      |         +-- wires: gpu.setMmu(mmu)
      |         +-- wires: gpu.setFrameBuffer(frameBuffer)
      |         +-- wires: mmu.addCartridge(cartridge)
      |         +-- wires: mmu.setCpu(cpu)
      |         +-- wires: gpu.setCpu(cpu)
      |         +-- cpu.reset(bootBios)
      |
      +-- creates LCDScreen(frameBuffer)
      +-- creates JFrame
      +-- enters outer loop
```

For headless testing, `TestEmulator` replaces `EmulatorApp`: it creates the FrameBuffer and Emulator directly, skipping JFrame, LCDScreen, and the outer loop. This validates that the core/presentation split works correctly.
