# Regression Analysis: Commit 3748f3e to HEAD

**Date:** 2026-03-10
**Baseline:** `3748f3e` ("First boot to tetris credits!!!")
**Intermediate:** `4e9e680` ("Interrupts working and tetris credits are clear")
**HEAD:** `6b50613` (current master)

## 1. Executive Summary

The diff between the known-working baseline (`3748f3e`) and HEAD spans 20 commits across two phases. Analysis reveals:

- **Phase 1** (3748f3e..4e9e680): 1 commit, 32 files, 213 insertions / 61 deletions. Contains the **most behavioral changes** — GPU `getControlInfo()` unhardcoded, interrupt infrastructure enabled, new opcodes, ALU flag fixes, and MMU memory model changes.
- **Phase 2** (4e9e680..HEAD): 18 commits, 111 files, 2216 ins / 880 del. Mostly infrastructure (Epics 1-2), the **GPU FSM was rewritten** (commit `3a040c6`), and 4 P1 bug fixes were applied.

**Highest-risk changes for graphical glitches:**
1. **GPU FSM rewrite** (3a040c6) — Replaced the 4-state machine (H_BLANK/V_BLANK/SPRITES/GRAPHICS) with a simplified line-counter model. This is the most likely cause of rendering issues and the primary target for Story 3.4.
2. **GPU `getControlInfo()` unhardcoded** (4e9e680) — Changed from hardcoded `return 145` to reading `MMU.LCD_CONTROLLER`. If the LCD controller register is corrupted by a write (e.g., via the I/O catch-all), this could cause rendering failures.
3. **GPU line initialization** (`line = 0` changed to `line = 1` in 3a040c6) — Alters scanline numbering, may cause off-by-one in rendering.

**Baseline Patch Experiment result:** Applying P1 fixes (Bugs 1, 3, 4) to the baseline did **not** resolve the graphical glitches. The intermittent blank frames persist identically. This confirms the GPU FSM is the primary investigation target for Story 3.4.

## 2. Phase 1 Analysis (3748f3e..4e9e680)

One commit adding interrupt support, new opcodes, and several behavioral fixes.

### GPU.java — Behavioral Change

| Change | Category | Risk |
|--------|----------|------|
| `getControlInfo()`: unhardcoded from `return 145` to `mmu.getByte(MMU.LCD_CONTROLLER)` | Behavioral | **HIGH** — now depends on correct LCD register state |
| Removed commented-out `return 129` alternative | Structural | None |

**Note:** The hardcoded `145` (0x91) equals the value set by `resetMemory()` at 0xFF40. For Tetris credits (which don't change the LCD controller), both behaviors are equivalent. But for games that modify LCD_CONTROLLER, the dynamic read is correct.

### MMU.java — Behavioral Changes

| Change | Category | Risk |
|--------|----------|------|
| `ram` array size: `int[0xffff]` to `int[0xffff + 1]` | **Bug fix** | LOW — fixes off-by-one, address 0xFFFF was inaccessible |
| HRAM: `zero[address - 0xFF80]` replaced with `ram[address]` for both read and write | Behavioral | MEDIUM — eliminates separate HRAM array, unifies into `ram[]` |
| Added `INTERRUPT_CONTROLLER_ADDRESS` write handler in `setByte()` | Behavioral | **HIGH** — enables interrupt flag persistence (was silently dropped before) |
| Added `INTERRUPT_CONTROLLER_ADDRESS` read handler in `getByte()` | Behavioral | **HIGH** — enables interrupt flag reading (returned 0 before) |
| `printVRAM()` formatting cleanup | Structural | None |

### Z80Cpu.java — Behavioral Changes

| Change | Category | Risk |
|--------|----------|------|
| 7 new opcodes registered (DEC_aHL, INC_A, INC_L, INC_aHL, RETI, RET_NZ, plus interrupt debug prints) | Behavioral | LOW — additive, needed for Tetris |
| `requestInterrupt()`: added debug `System.out.println` | Structural (debug) | None |
| `checkInterrupts()`: removed `System.exit(0)` — **interrupts now actually work** | **Critical behavioral** | **HIGH** — this is the single change that makes interrupts functional |
| `serveInterrupt()`: added debug `System.out.println` | Structural (debug) | None |
| Debug trace code changes (commented/uncommented `Operation.debug` blocks) | Structural (debug) | None |

### ALU.java — Behavioral Changes

| Change | Category | Risk |
|--------|----------|------|
| `decHI()`/`decLO()`: removed `boolean alterFlags` parameter, flags now always updated | Behavioral | MEDIUM — callers that passed `false` now get flag updates |
| `cpHI()`: changed from `updateDecFlags()` to `updateSubFlags()` | **Bug fix** | MEDIUM — `cpHI` is a compare (SUB without store), needs carry flag |
| `updateDecFlags()` made package-private (was private) | Structural | None |
| `updateIncFlags()` made package-private (was private) | Structural | None |
| New `updateSubFlags()` — same as `updateDecFlags()` but with carry flag | Behavioral | LOW — correct separation of DEC (no carry) vs SUB/CP (with carry) |

### Operation.java — Structural

| Change | Category | Risk |
|--------|----------|------|
| Added `print(String)` debug method | Structural | None |

### Emulator.java — Structural

| Change | Category | Risk |
|--------|----------|------|
| JFrame size adds `+ LCDScreen.FACTOR` padding | Structural (UI) | None |
| Added `totalCycles` counter and `screen.setCycles()` | Structural (display) | None |

### New Operation Files (Phase 1)

| File | Opcode | Description |
|------|--------|-------------|
| `OperationDEC_aHL.java` | 0x35 | DEC (HL) — decrement memory at HL |
| `OperationDEC_r.java` | (rename) | Renamed from `OperationDEC_N.java`, parameterized |
| `OperationINC_A.java` | 0x3C | INC A |
| `OperationINC_L.java` | 0x2C | INC L |
| `OperationINC_aHL.java` | 0x34 | INC (HL) — increment memory at HL |
| `OperationRETI.java` | 0xD9 | Return from interrupt (re-enables interrupts) |
| `OperationRET_NZ.java` | 0xC0 | Return if not zero |

### Existing Operations Modified (Phase 1)

| File | Change | Category |
|------|--------|----------|
| `OperationDEC_A/B/C/D/E.java` | Changed `alu.decHI/LO(reg, true)` to `alu.decHI/LO(reg)` | Follows ALU API change |
| `OperationAND_r.java` | Removed debug `if` block | Structural |
| `OperationCALL_nn.java` | Removed debug print | Structural |
| `OperationDI.java` | Removed debug print, added interrupt state display | Structural (debug) |
| `OperationEI.java` | Removed debug print | Structural |
| `OperationINC_rr.java` | Constructor parameter change | Structural |
| `OperationJP_Z_nn/JP_nn.java` | Removed debug prints | Structural |
| `OperationJR_cc_n.java` | Removed debug print | Structural |
| `OperationLDH_A_n/LDH_n_A.java` | Removed debug prints | Structural |
| `OperationPUSH_AF.java` | Removed unused import | Structural |
| `OperationPUSH_rr.java` | Removed debug print | Structural |
| `OperationRET_Z.java` | Changed constructor parameters, removed debug print | Structural |

## 3. Phase 2 Analysis (4e9e680..HEAD)

18 commits across Epics 1-2 and P1. Cross-referenced against P1 story file.

### GPU.java — Behavioral + Structural

| Change | Category | Source | Risk |
|--------|----------|--------|------|
| **FSM rewrite**: 4-state switch replaced with line-counter model | **Behavioral** | Commit `3a040c6` | **CRITICAL** — see Section 4 |
| `line` initial value: `0` to `1` | Behavioral | Commit `3a040c6` | HIGH — off-by-one |
| `pixels[][]` replaced with `FrameBuffer` | Structural | Story 2.1 | None |
| Static fields made instance (pixels, tiles removed) | Structural | Story 1.4 | None |
| Removed `drawScreen()` empty method | Structural | Cleanup | None |
| Removed `updateTile()` unused method | Structural | Cleanup | None |
| Removed debug comments | Structural | Story 1.2 | None |

### MMU.java — P1 Fixes + Structural

| Change | Category | Source | Risk |
|--------|----------|--------|------|
| `toSignedByte()` off-by-one fix + consolidation to BitUtils | **P1 Bug 1** | Story 3-P1 | None (fix) |
| HRAM `== 0xFF80` empty branch merged to `>= 0xFF80` | **P1 Bug 2** | Story 3-P1 | None (fix) |
| `setFrequency()`/`getTimerFrequency()` register fix | **P1 Bug 3** | Story 3-P1 | None (fix) |
| Timer register reads added to `getByte()` | **P1 additional** | Story 3-P1 | None (fix) |
| DMA transfer handler added | Behavioral | Post-4e9e680 | LOW |
| IO register (0xFF00) handler added | Behavioral | Post-4e9e680 | LOW |
| Constructor: removed GPU parameter, conditional BIOS read | Structural | Stories 2.1, 2.3 | None |
| Removed `video[]`, `io[]`, `zero[]` arrays | Structural | Unification | None |
| Fields made `final` | Structural | Story 1.4 | None |
| Removed `updatetile()`, `printVRAM()` | Structural | Cleanup | None |

### Z80Cpu.java — P1 Fix + Structural

| Change | Category | Source | Risk |
|--------|----------|--------|------|
| `isClockEnabled()` parameter swap fix | **P1 Bug 4** | Story 3-P1 | None (fix) |
| Opcode collision detection in `addOpcode()` | Structural | Story 2.2 | None |
| `OperationBIT_0_C` and `OperationINC_BC` added | Behavioral | Post-4e9e680 | LOW |
| `System.out.println` replaced with SLF4J | Structural | Story 1.2 | None |
| Debug trace blocks removed | Structural | Story 1.2 | None |
| `printLine` field removed | Structural | Story 1.2 | None |
| `printState()` replaced with `buildUnimplementedOpcodeMessage()` | Structural | Story 1.3 | None |
| Fields made `final`, static made `final` | Structural | Story 1.4 | None |
| `timerCounter` made instance (was static) | Structural | Story 1.4 | None |

### ALU.java — Structural Only (Phase 2)

| Change | Category | Source |
|--------|----------|--------|
| `cpu` field made `final` | Structural | Story 1.4 |
| Removed commented-out carry flag line | Structural | Cleanup |
| `setBIT()` renamed to `setBITFlags()` | Structural | Naming |

### Emulator.java — Structural Refactoring (D10 Verification)

| Change | Category | Source | Risk |
|--------|----------|--------|------|
| Extracted `initialize()` + `runFrame()` pattern from monolithic `run()` | Structural | Story 2.1 | None |
| Swing/JFrame code moved to `EmulatorApp` | Structural | Story 2.1 | None |
| Added `FrameBuffer` wiring | Structural | Story 2.1 | None |
| BIOS existence check added | Structural | Story 1.3 | None |

**D10 Verification:** The emulation loop (`runFrame()`) preserves identical behavior:
- Same cycle budget: `CYCLES_PER_FRAME = 4194304 / 60 = 69905`
- Same call order: `cpu.execute()` → `cpu.updateTimers(cycles)` → `cpu.checkInterrupts()` → `gpu.update(cycles)`
- Same cycle subtraction logic

## 4. GPU FSM Analysis — Critical for Story 3.4

### Before (3748f3e — 4-State FSM)

The baseline GPU implements the correct Game Boy LCD timing model:

```
SPRITES (80 cycles) → GRAPHICS (172 cycles) → H_BLANK (204 cycles) → next line
                                                                      └→ at line 144: V_BLANK
V_BLANK: 456 cycles per line, lines 144-153, then back to SPRITES at line 0
```

**Total per scanline:** 80 + 172 + 204 = 456 cycles (correct)
**Visible lines:** 0-143 (144 lines, correct)
**V-Blank lines:** 144-153 (10 lines, correct)
**drawLine() called in:** GRAPHICS → H_BLANK transition (correct — draw happens during GRAPHICS mode)

### Intermediate (4e9e680)

Same 4-state FSM as baseline. The only GPU change was unhardcoding `getControlInfo()`.

### After (HEAD — Simplified Line-Counter, commit `3a040c6`)

```
if (clock >= 456):
    if line < 145: drawLine(), V-Blank interrupt at 144, line++
    elif line >= 145 and line < 154: setScanline(0), line++
    else: line = 1
```

**Key differences:**
1. **No mode tracking** — the 4 LCD modes (H_BLANK, V_BLANK, SPRITES, GRAPHICS) are eliminated. The STAT register (0xFF41) cannot report the current mode, which breaks games that poll LCD status.
2. **drawLine() timing changed** — baseline draws at GRAPHICS→H_BLANK transition (172 cycles into the scanline). HEAD draws at the start of each 456-cycle period. This affects rendering relative to CPU writes.
3. **Line numbering off-by-one** — baseline starts at `line = 0`, HEAD starts at `line = 1`. The scanline register (LCD_LINE_COUNTER, 0xFF44) will be offset by 1 from hardware expectations.
4. **V-Blank scanline reporting** — baseline reports `line + 1` during V-Blank (lines 145-154 in the register). HEAD reports `0` for all V-Blank lines 145-153, then jumps to `1`. This means the scanline register shows `0` during V-Blank instead of the actual line.
5. **Missing `drawScreen()` call** — baseline called `drawScreen()` at the V-Blank transition (though it was empty). HEAD doesn't have this hook.
6. **V-Blank interrupt fires at line 144** — both versions fire V-Blank at line 144, which is correct.

### Assessment

The simplified FSM is **functionally incorrect** for the Game Boy hardware but was likely created as a workaround to get Tetris credits displaying ("better gpu timming"). For Story 3.4, restoring the 4-state FSM from the `4e9e680` baseline (not `3748f3e` — the `4e9e680` version has the unhardcoded `getControlInfo()`) is the recommended approach.

**However**, the graphical glitches (intermittent blank frames) exist in **both** the baseline (3748f3e with 4-state FSM) and HEAD (simplified model). This means the FSM simplification is not the direct cause of the glitches, but it introduces other correctness issues that will affect more complex games.

## 5. New Opcodes Inventory

### Added in Phase 1 (3748f3e..4e9e680)

| File | Opcode | Instruction | Notes |
|------|--------|-------------|-------|
| `OperationDEC_aHL.java` | 0x35 | DEC (HL) | New file |
| `OperationINC_A.java` | 0x3C | INC A | New file |
| `OperationINC_L.java` | 0x2C | INC L | New file |
| `OperationINC_aHL.java` | 0x34 | INC (HL) | New file |
| `OperationRETI.java` | 0xD9 | RETI | New file — critical for interrupt return |
| `OperationRET_NZ.java` | 0xC0 | RET NZ | New file |
| `OperationDEC_r.java` | — | DEC r | Renamed from `OperationDEC_N.java` |

### Added in Phase 2 (4e9e680..HEAD)

| File | Opcode | Instruction | Notes |
|------|--------|-------------|-------|
| `OperationBIT_0_C.java` | 0xCB 0x41 | BIT 0, C | New file |
| `OperationINC_BC.java` | 0x03 | INC BC | New file |
| `OperationBIT_b_r.java` | — | BIT b, r | Renamed from `OperationBIT_b_n.java` |

### Modified in Phase 2 (Structural only — debug removal, renaming)

Over 40 Operation files were modified in Phase 2, but all changes were structural: removing `debug` flag references (Story 1.2), removing debug prints, and parameter adjustments. No behavioral changes to existing opcodes.

## 6. Baseline Patch Experiment

### Methodology

1. **Extracted** commit `3748f3e` to `~/git/osom-old` using `git archive`
2. **Upgraded** build system: Java 8 → 17, Gradle wrapper updated, `build.gradle` modernized (plugins block, `implementation`/`testImplementation`, Lombok 1.18.26 with `annotationProcessor`)
3. **Applied 3 of 4 P1 fixes** (Bug 2 did not exist at this commit):
   - Bug 1: `toSignedByte()` off-by-one (`0xff - delta` → `0xff - delta + 1`) in MMU.java
   - Bug 3: `setFrequency()`/`getTimerFrequency()` register fix (`TIMER_ADDRESS` → `TIMER_CONTROLLER`) in MMU.java
   - Bug 4: `isClockEnabled()` parameter swap in Z80Cpu.java
4. **Bug 2 not applicable:** The baseline uses a separate `zero[]` array for HRAM with `address >= 0xFF80`. The empty `== 0xFF80` branch was introduced later when HRAM was consolidated into `ram[]`.
5. **Build:** `./gradlew clean build` — BUILD SUCCESSFUL
6. **Test:** Launched with `--rom roms/tetris.gb --no-bios`

### Additional Context: Interrupt `System.exit(0)`

The baseline has `System.exit(0)` in `checkInterrupts()` — the emulator terminates on the first served interrupt. This means:
- At `3748f3e`, interrupts were never actually served
- The V-Blank interrupt from GPU was requested but never processed (INTERRUPT_CONTROLLER_ADDRESS writes were also silently dropped in `setByte()`)
- Tetris credits worked purely through GPU rendering driven by the emulation loop, without interrupt-based frame synchronization

This `System.exit(0)` was **not** removed as part of the P1 patch experiment — it's not a P1 bug, and removing it would contaminate the experiment with non-P1 behavioral changes.

### Results

| Observation | Before Patch | After Patch |
|-------------|-------------|-------------|
| Tetris credits display | Yes | Yes |
| Intermittent blank frames (zeros) | Present | **Present — unchanged** |
| Crash | None | None |

### Conclusions

1. **P1 fixes do not resolve the graphical glitches.** The `toSignedByte` fix (which affects GPU tile rendering via `getByteSigned()`) and the timer fixes had no visible impact on the credits display.
2. **The glitches are intrinsic to the baseline GPU FSM** — they existed before any of our changes. The 4-state FSM at `3748f3e` already produced the intermittent blank frames.
3. **The timer bugs (3+4) had no effect** because: (a) `isClockEnabled()` was effectively dead code — timer register reads from `getByte()` returned 0 via the I/O catch-all, and (b) even if the timer had worked, the `System.exit(0)` in interrupt handling would have terminated the emulator.
4. **Bug 1 (`toSignedByte`)** may have been masked because Tetris credits use tile data area 0x8000 (unsigned mode), not 0x8800 (signed mode). The off-by-one only affects `getByteSigned()`, which is only called when `useUnsignedIdentifier = false`.

## 7. Recommendations for Epic 3

### Story 3.4 (GPU State Machine Restoration) — Highest Priority

1. **Restore the 4-state FSM** from commit `4e9e680` (which has the unhardcoded `getControlInfo()` and interrupt infrastructure), not from `3748f3e`.
2. **Investigate the blank frame glitch root cause** — it exists in the original 4-state FSM, so the FSM structure itself isn't the cause. Likely candidates:
   - The `renderBackground()` method reads the scanline from `MMU.LCD_LINE_COUNTER` — if the scanline register is stale or wrong during GRAPHICS mode, entire frames could render as zeros.
   - The scanline is set via `mmu.setScanline(line + 1)` at H_BLANK→SPRITES transition, but `drawLine()` happens at GRAPHICS→H_BLANK transition. There may be a timing mismatch where the scanline register doesn't match the actual line being drawn.
   - The `getControlInfo()` hardcoded value at `3748f3e` returns 145 (0x91) which is correct for boot, but checking if the dynamic read at `4e9e680` returns different values during rendering would be informative.
3. **Preserve FSM mode tracking** — even if the simplified model "works" for Tetris, the STAT register (0xFF41) needs mode information for other games.

### Story 3.3 (Timer System Verification)

The timer subsystem was effectively non-functional at the baseline due to compound bugs (I/O catch-all returning 0, wrong register, swapped params, `System.exit`). P1 fixed 3 of these issues, but additional issues remain (documented in P1 review follow-ups): `setByte()` write asymmetry for TIMA/TMA, frequency mask issue, stale TAC on enable-only toggle.

### Story 3.1 (Interrupt Signaling Refactor)

Phase 1 (4e9e680) made interrupts functional by: (1) adding INTERRUPT_CONTROLLER_ADDRESS handlers in `setByte()`/`getByte()`, (2) removing `System.exit(0)`, (3) adding RETI opcode. The current interrupt system works but routes through `Z80Cpu.requestInterrupt()` rather than the planned MMU-based routing (Architecture Decision D11).

### General

The P1 bug fixes are correct and necessary even though they didn't resolve the visible glitches. They fix real bugs that would surface as the emulator handles more complex games. The emulation core at HEAD is in a better state than at `3748f3e` in every dimension except the GPU FSM simplification.
