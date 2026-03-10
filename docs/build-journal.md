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
