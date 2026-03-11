# OSOM Build Journal

This is an append-only learning log documenting decisions, discoveries, and lessons learned during OSOM development. Each entry follows the Pattern 7 format.

**Entry format:**
```
### YYYY-MM-DD — [Topic]
**What:** One sentence describing what was investigated or fixed.
**Hardware concept:** Game Boy hardware behavior explanation (if applicable).
**What we learned:** Key insight.
**Changes:** Files modified and why.
```

---

### 2026-03-09 — Build Infrastructure Setup (Story 1.1)

**What:** Configured Gradle with Spock 2.4 testing framework, SLF4J 2.0.17/Logback 1.5.32 logging, and Groovy 4.0 plugin.

**What we learned:** The `useJUnitPlatform()` directive in `build.gradle` is critical for Spock test discovery. Without it, Spock tests compile but silently never run — Gradle reports 0 tests executed with no error. This is an easy mistake to miss because the build still succeeds.

**Changes:**
- `build.gradle` — Added Groovy plugin, Spock/Groovy/SLF4J/Logback dependencies, application plugin with `mainClass`, `useJUnitPlatform()`, removed JUnit 4
- `src/test/groovy/com/almejo/osom/` — Created test directory structure mirroring main source
- `src/main/resources/logback.xml` — Default INFO level for production
- `src/test/resources/logback-test.xml` — WARN level to prevent TRACE flood during tests

---

### 2026-03-09 — Logging Migration (Story 1.2)

**What:** Replaced all `System.out.println` calls with SLF4J structured logging via Lombok `@Slf4j`, and removed the legacy `Operation.debug` trace infrastructure.

**What we learned:** Removing the `Operation.debug` static boolean flag caused 58 compilation errors across Operation subclasses, because every subclass referenced it in conditional trace logic. Lesson: always verify the full impact of removing shared infrastructure before starting — a quick grep for usages would have revealed the scope immediately.

**Changes:**
- ~25 files updated with `@Slf4j` annotation and `log.*` calls replacing `System.out.println`
- `Operation.java` — Removed `debug` static flag and `print()` method
- All `Operation*` subclasses — Removed `debug` flag references
- ~50 fields across the codebase converted to `final` where possible during the sweep

---

### 2026-03-09 — CLI Improvements & Error Messages (Story 1.3)

**What:** Added `--log-level` runtime option, ROM/BIOS file validation with clear error messages, and diagnostic register dumps for unimplemented opcodes.

**What we learned:** Testing `System.exit()` in Java 17+ requires `SecurityManager`, which is terminally deprecated. The current approach works but produces deprecation warnings. Future stories should consider alternative patterns (e.g., refactoring exit logic into a testable method that returns an exit code, or using a process-based test that launches the JVM as a subprocess).

**Changes:**
- `Main.java` — Added `--log-level` option parsing, ROM/BIOS file existence checks with descriptive error messages
- `Z80Cpu.java` — Enhanced unimplemented opcode exception to include full register state dump (PC, SP, AF, BC, DE, HL, flags)
- `src/test/groovy/com/almejo/osom/CliImprovementsSpec.groovy` — New: 4 test cases covering missing ROM, invalid path, missing BIOS, log level, and unimplemented opcode diagnostics

---

### 2026-03-09 — Static State Refactor (Story 1.4)

**What:** Converted `GPU.pixels` and `Z80Cpu.timerCounter` from static fields to instance fields, enabling independent emulator instances for test isolation.

**Hardware concept:** The Game Boy has a single GPU and CPU, so the original code used `static` fields. However, for determinism testing (running two emulator instances and comparing output), each instance needs its own pixel buffer and timer state. Static fields mean all instances share the same state, making isolation impossible.

**What we learned:** Two key lessons: (1) Spock's `noExceptionThrown()` is a no-op assertion — it always passes regardless of what happens. Always use concrete value assertions like `cpu.@timerCounter == 1024`. (2) Discovered a pre-existing parameter-order bug: `isClockEnabled()` calls `BitUtils.isBitSetted(TIMER_ENABLED_BIT, value)` but the method signature is `isBitSetted(int value, int flag)` — the arguments are swapped, meaning the timer enable check doesn't work as intended.

**Changes:**
- `GPU.java` — Changed `private static final int[][] pixels` to `private final int[][] pixels`
- `Z80Cpu.java` — Changed `private static int timerCounter` to `private int timerCounter`; added `final` to `FLAG_ZERO`, `FLAG_SUBTRACT`, `FLAG_HALF_CARRY`, `FLAG_CARRY`
- `src/test/groovy/com/almejo/osom/gpu/GPUInstanceIsolationSpec.groovy` — New: GPU pixel array isolation test
- `src/test/groovy/com/almejo/osom/cpu/CpuInstanceIsolationSpec.groovy` — New: Z80Cpu timer counter isolation test with concrete value assertions

---

### 2026-03-09 — Core/Presentation Split (Story 2.1)

**What:** Separated the emulation core from the Swing UI layer, introducing FrameBuffer as the pixel buffer contract and creating a new `ui/` package for presentation code.

**Hardware concept:** On real Game Boy hardware, the GPU writes pixels to a framebuffer that the LCD reads independently. This refactoring mirrors that separation — `FrameBuffer` sits between GPU (writer) and LCDScreen (reader), matching the hardware's pixel buffer concept.

**What we learned:** When refactoring involves a dependency chain across multiple files (GPU → FrameBuffer → LCDScreen → EmulatorApp → Main), the code will not compile at intermediate steps. The tasks must be executed in dependency order, and compilation only succeeds once the full chain is wired. Planning the dependency graph upfront (as the story did) prevented wasted time on compilation errors.

**Changes:**
- `src/main/java/com/almejo/osom/gpu/FrameBuffer.java` — New: pixel buffer with `setPixel(x, y, color)` and `getPixels()`, WIDTH/HEIGHT constants
- `src/main/java/com/almejo/osom/gpu/GPU.java` — Replaced `int[][] pixels` field with `FrameBuffer frameBuffer` via `@Setter`; `renderBackground()` now calls `frameBuffer.setPixel()`
- `src/main/java/com/almejo/osom/Emulator.java` — Removed all Swing/AWT imports; extracted UI code to EmulatorApp; added `initialize()` + `runFrame()` pattern; accepts FrameBuffer and wires to GPU
- `src/main/java/com/almejo/osom/ui/EmulatorApp.java` — New: owns JFrame, outer loop, frame throttling, graceful shutdown via `volatile boolean running`
- `src/main/java/com/almejo/osom/ui/LCDScreen.java` — Moved from root package to `ui/`; constructor takes FrameBuffer instead of GPU; removed self-calling `repaint()`
- `src/main/java/com/almejo/osom/Main.java` — Changed to instantiate `EmulatorApp` instead of `Emulator`
- `src/main/java/com/almejo/osom/DataBus.java` — Deleted (empty unused placeholder)
- `src/test/groovy/com/almejo/osom/gpu/FrameBufferSpec.groovy` — New: 4 tests for pixel storage, dimensions, defaults, and instance isolation
- `src/test/groovy/com/almejo/osom/gpu/GPUInstanceIsolationSpec.groovy` — Updated to use FrameBuffer-based isolation pattern

---

### 2026-03-09 — Structural Tests (Story 2.2)

**What:** Created three structural test specs that enforce architectural boundaries and opcode registration integrity on every build.

**Hardware concept:** The Game Boy CPU uses a prefix byte (0xCB) to extend its instruction set — standard opcodes use one dispatch table, CB-prefixed opcodes use another. Mixing them would cause instruction decoding errors identical to real hardware bugs where the wrong instruction executes.

**What we learned:** Groovy's `@` operator for private field access (e.g., `cpu.@operations`) combined with Groovy collection methods like `groupBy` and `findAll` makes structural tests concise and readable. For the core/presentation boundary test, `File.eachFileRecurse(FileType.FILES)` is the idiomatic Groovy way to scan source directories. Using `Stub(MMU)` instead of a real MMU avoids needing the BIOS file during tests.

**Changes:**
- `src/test/groovy/com/almejo/osom/CorePresentationSeparationSpec.groovy` — New: 2 tests verifying no `javax.swing` or `java.awt` imports in core packages (`cpu/`, `memory/`, `gpu/`) and `Emulator.java`
- `src/test/groovy/com/almejo/osom/cpu/OpcodeUniquenessSpec.groovy` — New: 2 tests verifying all standard and CB-prefixed opcodes have unique codes with collision details in error messages
- `src/test/groovy/com/almejo/osom/cpu/CBPrefixSeparationSpec.groovy` — New: 2 tests verifying CB operations are only in the CB dispatch map and standard map contains no CB operations

---

### 2026-03-10 — Determinism Verification & Test Harness (Story 2.3)

**What:** Created a headless test harness (`TestEmulator`) for running the emulator without Swing, and a determinism verification test that proves two identical emulation runs produce the same framebuffer output.

**Hardware concept:** The Game Boy's execution is purely cycle-driven — no wall-clock, no RNG, no thread-dependent state. Given identical initial state and identical ROM input, the CPU, GPU, and memory produce identical output every time. This determinism is fundamental to the hardware and enables reliable regression testing.

**What we learned:** The `MMU` constructor was unconditionally reading `bios/bios.bin` even when `useBios=false`, making headless no-bios tests impossible without the BIOS file present. The fix (conditional read) was trivial but blocked all headless testing. Spock's `@Requires` annotation with a closure (e.g., `@Requires({ new File('roms/tetris.gb').exists() })`) cleanly handles tests that depend on gitignored resources — the test compiles and is discovered but skips gracefully when the resource is absent.

**Changes:**
- `src/main/java/com/almejo/osom/memory/MMU.java` — Made BIOS file read conditional on `useBios` flag; initializes empty `int[0]` array when `useBios=false`
- `src/test/groovy/com/almejo/osom/TestEmulator.groovy` — New: headless emulator helper with `runFrames(count)` and `computeFramebufferChecksum()` using CRC32
- `src/test/groovy/com/almejo/osom/DeterminismVerificationSpec.groovy` — New: conditional test comparing two 300-frame Tetris runs for identical CRC32 checksums and cycle counts

---

### 2026-03-10 — Architecture Documentation (Story 2.4)

**What:** Created developer-facing architecture documentation at `docs/architecture.md` describing the emulation loop, component responsibilities, data flow, memory map, and testing architecture.

**What we learned:** The Architecture Decision Document (ADD) in planning artifacts described the *planned* interrupt signaling mechanism (D11 — routing through MMU), but the *actual* code routes interrupt requests through `Z80Cpu.requestInterrupt()`. Writing documentation against the actual source code rather than planning artifacts caught this discrepancy. Lesson: always verify documentation against current code, not design documents — the code is the source of truth, especially during iterative development where not all planned decisions have been implemented yet.

**Changes:**
- `docs/architecture.md` — New: comprehensive architecture documentation covering overview, architecture layers, component responsibilities (Z80Cpu, Operation, ALU, MMU, GPU, FrameBuffer, Emulator, EmulatorApp, LCDScreen), main emulation loop, data flow diagram, subsystem communication map, memory map, CPU instruction architecture, interrupt system, timer subsystem, testing architecture, and object creation graph

---

### 2026-03-10 — Pre-existing Bug Fixes: MMU & Timer (Story 3-P1)

**What:** Fixed 4 pre-existing bugs in MMU and timer subsystem identified during the Epic 2 retrospective, plus 2 additional issues discovered during implementation.

**Hardware concept:** The Game Boy timer subsystem has 3 I/O registers: TIMA (0xFF05, counter), TMA (0xFF06, modulo), and TAC (0xFF07, controller — bits 0-1 select frequency, bit 2 enables timer). High RAM (HRAM, 0xFF80-0xFFFE) is 127 bytes of fast-access memory used by `LDH` instructions. Two's complement conversion for signed bytes follows: for `n > 127`, `signed = n - 256`.

**What we learned:**
1. **Bug origin tracing matters:** The `toSignedByte()` off-by-one existed independently in both `MMU.java` and `Operation.java`. The Operation copy was correct; the MMU copy was wrong. Having two independent implementations of the same formula is a duplication bug waiting to happen — consolidated both into `BitUtils.toSignedByte()` as a single source of truth.
2. **I/O register catch-all hides bugs:** `MMU.getByte()` had a catch-all `return 0` for all I/O registers in 0xFF01-0xFF7F not explicitly handled. This silently masked the timer register bugs — `isClockEnabled()` always received 0, making both the parameter-swap bug and the wrong-register bug invisible during execution. The catch-all needed explicit entries for timer registers (TIMA, TMA, TAC) and the divider register (0xFF04).
3. **Groovy `@` operator:** The `@` operator accesses private *fields* directly (e.g., `mmu.@ram[addr]`), but does NOT work on private *methods*. Groovy can call private methods directly without any special syntax (e.g., `cpu.isClockEnabled()`).

**Changes:**
- `src/main/java/com/almejo/osom/memory/MMU.java` — Bug 1: fixed `toSignedByte()` off-by-one (`0xff - delta` → `0xff - delta + 1`), then replaced with `BitUtils.toSignedByte()` call. Bug 2: merged empty `== 0xFF80` branch with `> 0xFF80` into `>= 0xFF80` for HRAM writes. Bug 3: changed `setFrequency()` and `getTimerFrequency()` to use `TIMER_CONTROLLER` instead of `TIMER_ADDRESS`. Additional: added timer registers and divider register to `getByte()` explicit read path.
- `src/main/java/com/almejo/osom/cpu/Z80Cpu.java` — Bug 4: swapped parameters in `isClockEnabled()` from `isBitSetted(TIMER_ENABLED_BIT, value)` to `isBitSetted(value, TIMER_ENABLED_BIT)`.
- `src/main/java/com/almejo/osom/cpu/BitUtils.java` — Added `toSignedByte(int value)` as shared utility.
- `src/main/java/com/almejo/osom/cpu/Operation.java` — Replaced inline `toSignedByte()` with delegation to `BitUtils.toSignedByte()`.
- `src/test/groovy/com/almejo/osom/memory/ToSignedByteSpec.groovy` — New: 7 test cases (0, 1, 127, 128→-128, 200→-56, 254→-2, 255→-1)
- `src/test/groovy/com/almejo/osom/memory/HighRAMSpec.groovy` — New: 4 test cases (write/read 0xFF80, 0xFF81, 0xFFFE, default zero)
- `src/test/groovy/com/almejo/osom/memory/TimerFrequencySpec.groovy` — New: 3 test cases (TAC write doesn't corrupt TIMA, TAC stores correctly, frequency detection reads TAC not TIMA)
- `src/test/groovy/com/almejo/osom/cpu/TimerEnableSpec.groovy` — New: 4 test cases (TAC bit 2 clear/set, 0x04, 0x00)
- `src/main/java/com/almejo/osom/cpu/OperationJR_cc_n.java` — Added `static import BitUtils.toSignedByte` replacing inherited wrapper method
- `src/test/groovy/com/almejo/osom/cpu/ToSignedByteSpec.groovy` — Moved from `memory/` to `cpu/`, now tests `BitUtils.toSignedByte()` directly

---

### 2026-03-10 — Regression Diagnosis & Git Forensics (Story 3-P2)

**What:** Analyzed all behavioral changes between commit `3748f3e` ("First boot to tetris credits!!!") and HEAD, recreated the historical baseline, applied P1 bug fixes, and tested whether Tetris graphical glitches were resolved.

**Hardware concept:** The Game Boy GPU cycles through 4 modes per scanline: OAM Search (mode 2, 80 cycles), Pixel Transfer (mode 3, 172 cycles), H-Blank (mode 0, 204 cycles), and V-Blank (mode 1, 4560 cycles total for lines 144-153). Each scanline takes exactly 456 cycles. The LCD STAT register (0xFF41) reports the current mode, which games use for timing-critical VRAM writes.

**What we learned:**
1. **GPU FSM was rewritten in commit `3a040c6`** ("better gpu timming") — the correct 4-state machine was replaced with a simplified line-counter that fires every 456 cycles without mode tracking. This eliminates LCD mode reporting and changes draw timing. Story 3.4 should restore the 4-state FSM from commit `4e9e680` (not `3748f3e`, which had a hardcoded `getControlInfo()`).
2. **P1 fixes don't fix the graphical glitches.** Applying Bugs 1, 3, 4 to the baseline produced no visible change in Tetris credits rendering. The intermittent blank frames (zeros alternating with the display) persist. This means the glitches are intrinsic to the GPU rendering logic, not caused by MMU/timer bugs.
3. **Bug 2 (0xFF80 write discard) didn't exist at `3748f3e`.** The baseline used a separate `zero[]` array for HRAM with `>= 0xFF80`. The empty branch was introduced later when HRAM was consolidated into `ram[]`. Only 3 of 4 P1 fixes were applicable to the baseline.
4. **Interrupts were non-functional at `3748f3e`.** `checkInterrupts()` had `System.exit(0)` — the emulator terminated on the first interrupt. Additionally, `INTERRUPT_CONTROLLER_ADDRESS` had no handler in `setByte()`/`getByte()`, so interrupt flags were silently dropped. The timer bugs were invisible because the I/O catch-all returned 0 for all timer registers. Tetris credits worked purely through cycle-driven GPU rendering without any interrupt-based synchronization.
5. **`getControlInfo()` was hardcoded to `return 145`** at `3748f3e`. This was changed to read `MMU.LCD_CONTROLLER` in `4e9e680`. The hardcoded value (0x91) matches the boot initialization, so it's functionally equivalent for Tetris credits but would break any game that modifies the LCD controller register.

**Changes:**
- `docs/regression-analysis.md` — New: comprehensive 7-section analysis document
- `~/git/osom-old/` — Recreated from `git archive 3748f3e`, upgraded build.gradle for Java 17/Gradle 7.6, P1 Bugs 1/3/4 applied
- `docs/build-journal.md` — This entry

---

### 2026-03-10 — Pan Docs Opcode Reference Table (Story 3-P3)

**What:** Created complete opcode reference table from Pan Docs/gb-opcodes JSON data with implementation status tracking.

**Hardware concept:** The LR35902 CPU has 256 standard opcodes (0x00-0xFF) and 256 CB-prefixed opcodes (0xCB 0x00-0xCB 0xFF). The CB prefix byte (0xCB) extends the instruction set, primarily for bit manipulation, rotation, and shift operations. Each opcode has a fixed byte length (1-3), fixed or conditional cycle count, and defined flag effects on Z (Zero), N (Subtract), H (Half-Carry), and C (Carry).

**What we learned:**
1. **Coverage is 95/512 (18.6%).** 90 standard opcodes + 5 CB-prefixed opcodes are currently registered. The 0xCB byte itself acts as a prefix router in the emulator, not a standalone instruction.
2. **The gb-opcodes JSON at gbdev.io/gb-opcodes is authoritative and machine-readable.** It separates mnemonic from operands, with `immediate: false` indicating memory-indirect operands (e.g., `(HL)` rather than `HL`). Conditional instructions have a two-element `cycles` array (taken/not-taken).
3. **Existing parameterized base classes cover many missing opcodes.** Classes like `OperationLD_r_n`, `OperationINC_r`, `OperationDEC_r` accept a register parameter — new opcodes can often be added by registering a new instance rather than writing a new class. This will accelerate Story 3.5 (Iterative Opcode Implementation).
4. **Pan Docs and gb-opcodes are CC0 (public domain).** Attribution is included as professional courtesy, not legal requirement.

**Changes:**
- `docs/opcode-reference.md` — New: complete opcode reference with 512 entries and implementation status
- `docs/build-journal.md` — This entry

---

### 2026-03-10 — Interrupt Signaling Refactor (Story 3.1)

**What:** Moved interrupt signaling from Z80Cpu to MMU, fixed critical V-Blank bug, added serial interrupt support, implemented EI one-instruction delay, and fixed checkInterrupts to serve only one interrupt per call.

**Hardware concept:** The Game Boy has 5 interrupt sources: V-Blank (bit 0, handler 0x40), LCD STAT (bit 1, 0x48), Timer (bit 2, 0x50), Serial (bit 3, 0x58), and Joypad (bit 4, 0x60). Interrupt requests are signaled by setting bits in the IF register (0xFF0F). The CPU serves interrupts in priority order (V-Blank highest) when both IF and IE (0xFFFF) bits are set and the IME flag is true. The EI instruction enables interrupts after a one-instruction delay — this prevents an interrupt from firing between EI and the instruction that follows it (commonly `EI; RET` or `EI; HALT`).

**What we learned:**
1. **Critical V-Blank bug:** GPU was passing the handler ADDRESS (0x40 = 64 decimal) as a bit INDEX to `BitUtils.setBit()`, resulting in `setBit(value, 64)` — a completely wrong bit position. V-Blank interrupts never worked correctly. The fix routes through `MMU.requestInterrupt(MMU.INTERRUPT_VBLANK)` which correctly passes bit index 0.
2. **checkInterrupts served multiple interrupts per call.** The original loop iterated through all 5 bits without stopping after serving one. Since `serveInterrupt()` disables IME, subsequent loop iterations would still call `canServeInterrupt()` (which only checks IF/IE bits, not IME). This caused the CPU to serve multiple interrupts in a single `checkInterrupts()` call, corrupting the stack. Fixed by adding `return` after the first served interrupt.
3. **Missing serial interrupt handler.** The `INTERRUPT_ADDRESSES` map had no entry for bit 3 (serial). If a serial interrupt was requested, `INTERRUPT_ADDRESSES.get(3)` would return `null`, causing a NullPointerException. Added `INTERRUPT_ADDRESS_SERIAL = 0x58`.
4. **Decoupling GPU from CPU.** After routing interrupt signaling through MMU, the GPU no longer references Z80Cpu at all. Removed the `cpu` field and `setCpu()` from GPU, eliminating a coupling vector. GPU now only depends on MMU and FrameBuffer.

**Changes:**
- `src/main/java/com/almejo/osom/memory/MMU.java` — Added `requestInterrupt(int bit)` method and 5 named interrupt constants (`INTERRUPT_VBLANK` through `INTERRUPT_JOYPAD`); added `@Slf4j`
- `src/main/java/com/almejo/osom/cpu/Z80Cpu.java` — Removed `requestInterrupt()` method and 4 interrupt bit constants; added `INTERRUPT_ADDRESS_SERIAL = 0x58`; updated `INTERRUPT_ADDRESSES` map to use MMU constants; added `pendingInterruptEnable` field and setter; EI delay check at start of `execute()`; timer overflow now calls `mmu.requestInterrupt(MMU.INTERRUPT_TIMER)`; `checkInterrupts()` returns after serving first interrupt
- `src/main/java/com/almejo/osom/gpu/GPU.java` — Removed `cpu` field and `Z80Cpu` import; V-Blank now calls `mmu.requestInterrupt(MMU.INTERRUPT_VBLANK)`
- `src/main/java/com/almejo/osom/cpu/OperationEI.java` — Changed to `cpu.setPendingInterruptEnable(true)` for delayed enable
- `src/main/java/com/almejo/osom/cpu/BitUtils.java` — Made `setBit()` public (needed by MMU cross-package)
- `src/main/java/com/almejo/osom/Emulator.java` — Removed `gpu.setCpu(cpu)` call
- `src/test/groovy/com/almejo/osom/memory/InterruptSignalingSpec.groovy` — New: 8 tests for `MMU.requestInterrupt()` and interrupt constants
- `src/test/groovy/com/almejo/osom/cpu/InterruptHandlingSpec.groovy` — New: 9 tests for checkInterrupts priority, serveInterrupt sequence, EI delay, DI immediate, RETI immediate, IME guard, handler addresses
