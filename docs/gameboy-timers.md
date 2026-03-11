# Game Boy Timer System

Reference document for understanding how timers work in the LR35902 (Game Boy CPU).

## What Timers Are and Why They Exist

Games need things to happen at specific speeds — pieces fall in Tetris, music plays at a tempo, animations advance at a rate. Without a timer, the game would have to count CPU cycles manually:

```asm
gravity_wait:
    dec b               ; Decrement counter
    jr nz, gravity_wait ; Loop until zero
```

This "busy wait" wastes CPU cycles, scales unpredictably, and makes timing depend on how many instructions the game runs per frame. **Hardware timers** solve this: the timer counts independently of what the CPU is doing. When it reaches a threshold, it fires an interrupt — "time's up, do the thing."

## The Four Timer Registers

The Game Boy timer system uses four memory-mapped registers:

| Address | Name | Purpose |
|---------|------|---------|
| `$FF04` | **DIV** | Divider Register — always counting, read-only counter |
| `$FF05` | **TIMA** | Timer Counter — the programmable timer |
| `$FF06` | **TMA** | Timer Modulo — reload value after overflow |
| `$FF07` | **TAC** | Timer Control — speed and on/off switch |

### DIV — The Free-Running Clock ($FF04)

DIV increments at 16384 Hz — once every 256 T-cycles. It counts from `$00` to `$FF`, wraps around, and keeps going. **Always.** You cannot stop it. You cannot change its speed.

Writing **any value** to `$FF04` resets DIV to `$00`. The actual value you write doesn't matter — the act of writing is the reset.

```asm
    ldh [$FF04], a      ; Reset DIV to 0 (value of A is irrelevant)
```

**What games use it for:** Quick-and-dirty random numbers. Since DIV is always counting, reading it at unpredictable moments (like when the player presses a button) gives a pseudo-random value:

```asm
    ldh a, [$FF04]      ; Read DIV — effectively random
    and $03             ; Mask to 0-3 for piece selection
```

Tetris uses DIV this way for piece randomization.

### TIMA — The Programmable Timer ($FF05)

TIMA is the register that actually "times" things. It counts up at a speed set by TAC. When it overflows past `$FF`, three things happen simultaneously:

1. TIMA reloads from TMA (the modulo register)
2. A **Timer interrupt** is requested (bit 2 of IF register)
3. The game's timer interrupt handler runs

TIMA can be read and written by the game. Writing to TIMA sets it to a specific starting value — useful for counting a precise number of ticks before the interrupt fires.

### TMA — The Reload Value ($FF06)

When TIMA overflows, hardware copies TMA into TIMA automatically. This determines the **period** of the timer:

- TMA = `$00` → TIMA counts 256 ticks before overflow (longest period)
- TMA = `$FF` → TIMA counts 1 tick before overflow (shortest period)
- TMA = `$F0` → TIMA counts 16 ticks before overflow

The formula: `ticks_before_interrupt = 256 - TMA`

### TAC — Speed and Enable ($FF07)

```
Bit 7-3: Unused
Bit 2:   Timer Enable (1 = TIMA counting, 0 = TIMA frozen)
Bit 1-0: Clock Select (speed)
```

**Clock Select values:**

| Bits 1-0 | Frequency | T-cycles per TIMA tick | M-cycles per tick |
|----------|-----------|----------------------|-------------------|
| `00` | 4096 Hz | 1024 | 256 |
| `01` | 262144 Hz | 16 | 4 |
| `10` | 65536 Hz | 64 | 16 |
| `11` | 16384 Hz | 256 | 64 |

**Important:** DIV always counts regardless of TAC bit 2. Only TIMA is affected by the enable bit.

## How the Timer Hardware Actually Works

Understanding the real hardware helps avoid subtle emulation bugs.

### The Internal 16-bit System Counter

Internally, the Game Boy maintains a **16-bit counter** that increments every T-cycle (at 4,194,304 Hz). DIV is just the **upper 8 bits** of this counter — that's why it increments at 16384 Hz (4,194,304 / 256 = 16,384).

```
System Counter (16-bit, increments every T-cycle):
┌───────────────┬───────────────┐
│  Bits 15-8    │   Bits 7-0    │
│  (DIV = $FF04)│  (not visible)│
└───────────────┴───────────────┘
```

When you write to `$FF04`, it resets the **entire** 16-bit counter to `$0000` — not just the upper 8 bits.

### The Falling Edge Detector

TIMA does not simply "count cycles." Instead, a **multiplexer** selects one specific bit from the 16-bit system counter based on TAC's clock select:

| TAC bits 0-1 | Selected bit | Frequency |
|--------------|-------------|-----------|
| `00` | Bit 9 | 4096 Hz |
| `01` | Bit 3 | 262144 Hz |
| `10` | Bit 5 | 65536 Hz |
| `11` | Bit 7 | 16384 Hz |

This selected bit is **ANDed** with the TAC enable bit (bit 2). The result feeds a **falling edge detector**: TIMA increments only when this combined signal transitions from 1 to 0.

```
System Counter Bit (selected by TAC) ─┐
                                       ├─ AND ─── Falling Edge Detector ─── TIMA++
TAC Enable Bit ────────────────────────┘
```

This mechanism means:
- When the timer is enabled and the counter rolls through the selected bit, TIMA ticks
- When you **write to DIV** (resetting the counter), the selected bit may go from 1 to 0 — causing a spurious TIMA tick
- When you **disable the timer** (clear TAC bit 2) while the selected bit is 1, the AND result goes from 1 to 0 — causing a spurious TIMA tick (DMG only)

### Emulator Simplification

For a basic emulator targeting Tetris compatibility, you don't need to implement the falling edge detector precisely. A simpler approach works:

1. Track a **cycle accumulator** per timer frequency
2. Decrement it by the number of T-cycles each instruction consumed
3. When it reaches zero, increment TIMA and reload the accumulator
4. On TIMA overflow: reload from TMA, fire interrupt

The falling edge quirks matter for test ROM accuracy (Blargg's, Mooneye), but no commercial game depends on them for correct gameplay.

## Timer Overflow — Step by Step

When TIMA overflows past `$FF`:

```
Before overflow:
  TIMA = $FF, TMA = $E0

TIMA tick arrives:
  TIMA increments: $FF + 1 = $100 → overflow detected

Hardware response (within 1 M-cycle):
  1. TIMA ← TMA ($E0)
  2. IF bit 2 ← 1  (Timer interrupt requested)

After overflow:
  TIMA = $E0  (counting resumes from here)
  IF = xxxxxx1xx  (Timer interrupt pending)
```

The Timer interrupt handler at `$0050` then runs (if IME and IE allow it).

## How Games Use Timers

### Tetris: Gravity and Game Speed

Tetris uses the Timer interrupt to control how fast pieces fall. At level 0, pieces drop approximately once per 48 frames. The timer counts in the background; when it fires, the interrupt handler advances the piece down one row.

```asm
; Timer setup for level-0 gravity
    ld a, $00           ; TMA = 0 → 256 ticks per overflow
    ldh [$FF06], a
    ld a, $04           ; TAC = timer enabled, 4096 Hz
    ldh [$FF07], a
    ; Timer fires every 256/4096 = 0.0625 seconds ≈ 16Hz
```

As the level increases, TMA increases (shorter period = faster gravity):

```asm
; Faster gravity for higher levels
    ld a, $C0           ; TMA = $C0 → 64 ticks per overflow
    ldh [$FF06], a      ; Timer fires 4x faster
```

### Music Tempo

Sound engines use the timer to advance music at a steady tempo independent of game logic complexity:

```asm
TimerHandler:
    push af
    call AdvanceMusicTick   ; Next note/envelope step
    pop af
    reti
```

### Precise Delays

Some games set TIMA to a specific value to wait an exact number of ticks:

```asm
    ld a, $F0           ; Start counting from $F0
    ldh [$FF05], a      ; 16 ticks until overflow
    ld a, $05           ; Timer enabled, 262144 Hz
    ldh [$FF07], a      ; Each tick = ~3.8 microseconds
    ; Total delay = 16 × 3.8μs ≈ 61 microseconds
```

## Computing Timer Interrupt Period

The time between Timer interrupts depends on three values:

```
CPU clock = 4,194,304 Hz (T-cycles per second)

Ticks per overflow = 256 - TMA

T-cycles per tick (from TAC):
  00 → 1024
  01 → 16
  10 → 64
  11 → 256

T-cycles per interrupt = ticks_per_overflow × T_cycles_per_tick
Seconds per interrupt  = T_cycles_per_interrupt / 4,194,304
Interrupts per second  = 4,194,304 / T_cycles_per_interrupt
```

**Examples:**

| TMA | TAC | Ticks | T-cycles/tick | T-cycles/interrupt | Rate |
|-----|-----|-------|---------------|-------------------|------|
| `$00` | `$04` | 256 | 1024 | 262,144 | ~16 Hz |
| `$00` | `$05` | 256 | 16 | 4,096 | ~1024 Hz |
| `$F0` | `$04` | 16 | 1024 | 16,384 | ~256 Hz |
| `$FF` | `$07` | 1 | 256 | 256 | ~16384 Hz |

## DIV vs TIMA — When to Use Which

| Feature | DIV ($FF04) | TIMA ($FF05) |
|---------|-------------|--------------|
| Speed | Fixed 16384 Hz | Configurable (4 speeds) |
| Can stop? | No | Yes (TAC bit 2) |
| Interrupt? | No | Yes (on overflow) |
| Writable? | Only reset to 0 | Yes, any value |
| Configurable period? | No | Yes (via TMA) |
| Use case | Random seeds, rough timing | Precise game timing, music |

## Timer Initialization at Boot

After the boot ROM completes (or when starting with `--no-bios`), the timer registers have these values:

| Register | Post-boot value |
|----------|----------------|
| DIV ($FF04) | `$AB` (DMG), varies by model |
| TIMA ($FF05) | `$00` |
| TMA ($FF06) | `$00` |
| TAC ($FF07) | `$00` (timer disabled) |

The timer is **disabled** by default. Games must explicitly enable it by writing to TAC with bit 2 set.

## Summary Diagram

```
                            4,194,304 Hz
                                 │
                                 v
                    ┌────────────────────────┐
                    │  16-bit System Counter  │
                    │  (increments every      │
                    │   T-cycle)              │
                    └────────────────────────┘
                      │                    │
                      │ Bits 15-8          │ Bit selected by
                      │                    │ TAC clock select
                      v                    v
                  ┌────────┐         ┌──────────┐
         Read ──> │  DIV   │         │   MUX    │ <── TAC bits 0-1
         $FF04    │ ($FF04)│         │          │
                  └────────┘         └──────────┘
                      │                    │
          Write any   │                    │
          value ──────┘                    v
          resets entire              ┌──────────┐
          16-bit counter             │ AND gate │ <── TAC bit 2 (enable)
                                     └──────────┘
                                           │
                                           v
                                     ┌──────────┐
                                     │ Falling  │
                                     │  Edge    │  1→0 transition
                                     │ Detector │  triggers tick
                                     └──────────┘
                                           │
                                           v
                                     ┌──────────┐
                              Read/  │  TIMA    │  Counts up
                              Write  │ ($FF05)  │
                                     └──────────┘
                                           │
                                      Overflow?
                                      ($FF → $00)
                                       /       \
                                     YES        NO
                                      │          │
                                      v          └── keep counting
                                ┌──────────┐
                         Read/  │  TMA     │  Reload value
                         Write  │ ($FF06)  │  copied to TIMA
                                └──────────┘
                                      │
                                      v
                              ┌───────────────┐
                              │ Timer Interrupt│
                              │ IF bit 2 = 1  │
                              │ Vector: $0050  │
                              └───────────────┘
```

## References

- [Pan Docs — Timer and Divider Registers](https://gbdev.io/pandocs/Timer_and_Divider_Registers.html)
- [Pan Docs — Timer Obscure Behaviour](https://gbdev.io/pandocs/Timer_Obscure_Behaviour.html)
- [Pan Docs — Interrupt Sources](https://gbdev.io/pandocs/Interrupt_Sources.html)
- [GBEDG — Timer Technical Reference](https://github.com/Hacktix/GBEDG/blob/master/timers/index.md)
- [Imran Nazar — GameBoy Emulation: Timers](https://imrannazar.com/GameBoy-Emulation-in-JavaScript:-Timers)
