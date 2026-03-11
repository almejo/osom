# Game Boy Interrupt System

Reference document for understanding how interrupts work in the LR35902 (Game Boy CPU).

## What Are Interrupts and Why They Exist

Without interrupts, game code would have to **poll** — constantly check every hardware register in a tight loop:

```asm
main_loop:
    ld a, [$FF44]       ; Read current scanline
    cp 144              ; Are we in VBlank yet?
    jr nz, main_loop    ; No? Keep checking...
```

This wastes CPU cycles and misses events. **Interrupts** let peripherals (screen, timer, serial port, joypad) tap the CPU on the shoulder: "something happened." The CPU pauses, saves its place, jumps to a handler routine, deals with the event, and resumes exactly where it left off.

## The Five Interrupt Types

| Bit | Interrupt | Vector | Trigger |
|-----|-----------|--------|---------|
| 0 | **V-Blank** | `$0040` | PPU enters vertical blanking period (scanline 144) |
| 1 | **LCD STAT** | `$0048` | Various LCD status conditions (configurable) |
| 2 | **Timer** | `$0050` | Timer counter (TIMA) overflows past `$FF` |
| 3 | **Serial** | `$0058` | Serial data transfer completes (8 bits shifted) |
| 4 | **Joypad** | `$0060` | A button input pin transitions from high to low |

### V-Blank (INT $40) — The Most Important

The LCD draws 144 visible scanlines. After line 143, hardware enters **vertical blanking** (lines 144-153, ~1.1ms). During this window, VRAM is free — your code can safely write to it. Fires ~59.7 times/second. Nearly every game uses this as its main loop heartbeat.

### LCD STAT (INT $48) — Mid-Frame Effects

Fires on configurable LCD conditions: H-Blank (mode 0), V-Blank (mode 1), OAM search (mode 2), or LYC=LY match. Used for "raster effects" — changing scroll/palette mid-frame for things like a non-scrolling status bar.

### Timer (INT $50) — Precision Timing

TIMA register ($FF05) counts up at a selectable frequency (4096, 16384, 65536, or 262144 Hz). On overflow, reloads from TMA ($FF06) and fires this interrupt. Used for music tempo, animation timing.

### Serial (INT $58) — Link Cable

Fires when 8-bit serial transfer completes. Used for multiplayer (Tetris, Pokemon link trading).

### Joypad (INT $60) — Button Presses

Fires on button press (high-to-low transition). **Almost no games use this** — buttons bounce, making it unreliable. Games poll the joypad during V-Blank instead. Exists primarily to wake the CPU from `STOP` mode.

## The Three Controlling Registers

An interrupt fires only when **all three gates** are open:

```
IME == 1  AND  IE bit == 1  AND  IF bit == 1
```

### IF — Interrupt Flag Register ($FF0F)

```
Bit 7-5: Unused (read as 1)
Bit 4:   Joypad    (1 = requested)
Bit 3:   Serial    (1 = requested)
Bit 2:   Timer     (1 = requested)
Bit 1:   LCD STAT  (1 = requested)
Bit 0:   V-Blank   (1 = requested)
```

Records which interrupts hardware has **requested**. Bits are set automatically by hardware on the rising edge (transition from 0 to 1). Bits remain set until cleared — either by the CPU servicing the interrupt, or by software writing to IF.

Software **can** write to IF: writing `1` triggers an interrupt artificially, writing `0` clears a pending one.

### IE — Interrupt Enable Register ($FFFF)

```
Bit 7-5: Unused
Bit 4:   Joypad    (1 = enabled)
Bit 3:   Serial    (1 = enabled)
Bit 2:   Timer     (1 = enabled)
Bit 1:   LCD STAT  (1 = enabled)
Bit 0:   V-Blank   (1 = enabled)
```

A **mask** controlling which interrupts the program is willing to handle. An interrupt can be requested (IF bit set) even if disabled (IE bit clear) — the request waits. Enabling it later fires it immediately.

### IME — Interrupt Master Enable (CPU-internal)

Global on/off switch for the entire system. **Not memory-mapped** — you cannot read it at any address.

Controlled by:
- `EI` ($FB): Sets IME = 1 **after the next instruction** (delayed!)
- `DI` ($F3): Sets IME = 0 **immediately**
- `RETI` ($D9): Pops PC from stack AND sets IME = 1 **immediately** (no delay)
- Interrupt dispatch itself: Clears IME = 0

## The Complete Interrupt Lifecycle

### Phase 1: The Request

Hardware event occurs (e.g., scanline reaches 144). Hardware sets the corresponding bit in IF to 1.

### Phase 2: The Check (between every instruction)

After each instruction, before fetching the next:

```
pending = IF & IE
if (IME == 1 && pending != 0):
    service highest-priority pending interrupt
```

### Phase 3: The Dispatch (5 M-cycles / 20 T-cycles)

The CPU performs this automatically — the programmer does not write this code:

| M-cycle | Action |
|---------|--------|
| 1 | Internal delay. CPU decodes which interrupt. IME cleared to 0. |
| 2 | Internal delay. |
| 3 | Push PC high byte. SP decremented. |
| 4 | Push PC low byte. SP decremented again. IF bit cleared. |
| 5 | Load vector address into PC. |

### Phase 4: The Handler

Your code at the vector address runs. At this point:
- IME is 0 (no nested interrupts unless you explicitly `EI`)
- The relevant IF bit has been cleared
- The return address is on the stack

### Phase 5: The Return

`RETI` pops the saved PC and sets IME = 1. Normal execution resumes.

## How Game Code Uses Interrupts

### Setting Up Interrupt Vectors

Vectors are at fixed addresses, spaced 8 bytes apart — only room for a `JP` or `RETI`:

```asm
SECTION "VBlank Handler", ROM0[$0040]
    jp VBlankRoutine        ; Jump to full handler

SECTION "LCD STAT Handler", ROM0[$0048]
    reti                    ; Not used

SECTION "Timer Handler", ROM0[$0050]
    jp TimerRoutine

SECTION "Serial Handler", ROM0[$0058]
    reti                    ; Not used

SECTION "Joypad Handler", ROM0[$0060]
    reti                    ; Not used
```

### Initialization

```asm
Init:
    di                      ; Disable interrupts during setup

    ; ... set up graphics, load tiles, etc. ...

    ld a, %00000101         ; Enable V-Blank (bit 0) and Timer (bit 2)
    ldh [$FFFF], a          ; Write to IE register

    xor a                   ; A = 0
    ldh [$FF0F], a          ; Clear all pending IF flags

    ei                      ; Enable IME (takes effect after next instruction)
```

### A Real V-Blank Handler

```asm
VBlankRoutine:
    push af                 ; Save registers
    push bc
    push de
    push hl

    ld a, $C0              ; OAM DMA source high byte
    ldh [$FF46], a          ; Start DMA transfer

    call ReadJoypad         ; Poll joypad (not interrupt-driven)
    call UpdateGameLogic

    ld a, 1
    ld [$C100], a           ; Set vblank_done flag

    pop hl                  ; Restore registers
    pop de
    pop bc
    pop af
    reti                    ; Return + re-enable interrupts
```

### The Main Loop Pattern

```asm
MainLoop:
    halt                    ; CPU sleeps until (IE & IF) != 0
    nop                     ; Safety NOP (HALT bug on some revisions)

    ld a, [$C100]           ; Check vblank_done flag
    or a
    jr z, MainLoop

    xor a
    ld [$C100], a           ; Clear flag

    call ProcessInput
    call UpdateAnimations
    jr MainLoop
```

`HALT` stops the CPU from fetching instructions, entering low-power mode. It wakes the instant `IE & IF` becomes non-zero.

### How Tetris Uses V-Blank

Tetris sets `IE = $01` (only V-Blank enabled). Its main loop:
1. Clears a VBlank flag and calls `HALT`
2. V-Blank fires, handler runs (OAM DMA, reads joypad), sets flag
3. CPU wakes, main loop sees flag set, runs game logic
4. Repeat

Tetris does **not** use the joypad interrupt — it polls during V-Blank like most games.

## Priority System

Multiple pending interrupts are serviced in bit order, lowest first:

| Priority | Bit | Interrupt |
|----------|-----|-----------|
| Highest | 0 | V-Blank |
| | 1 | LCD STAT |
| | 2 | Timer |
| | 3 | Serial |
| Lowest | 4 | Joypad |

No interrupt is lost — lower-priority ones wait their turn. After the V-Blank handler `RETI`s, the CPU checks again and finds Timer still pending, services it, and so on.

## The EI Delay

`EI` enables interrupts **after the next instruction finishes**. This is deliberate.

### Why It Exists

```asm
    di                  ; Disable interrupts for critical section
    ; ... modify shared data ...
    ei                  ; Re-enable
    ret                 ; Return to caller
```

If `EI` were immediate, an interrupt could fire between `EI` and `RET`, corrupting the stack. With the delay, `RET` executes first, and the interrupt fires after the function cleanly returns.

### EI + HALT Pattern (Standard)

```asm
    ei                  ; IME scheduled to become 1 after next instruction
    halt                ; HALT is "the next instruction" — then IME becomes 1
```

This atomically enables interrupts and sleeps. The CPU wakes and services the interrupt.

### EI + DI

```asm
    ei                  ; Schedule IME = 1
    di                  ; IME = 0 immediately (overrides pending EI)
```

No interrupts can fire between these two. To allow one interrupt:

```asm
    ei
    nop                 ; IME becomes 1 here — interrupt CAN fire
    di
```

### RETI Has No Delay

Unlike `EI`, `RETI` sets IME = 1 **immediately**. This is safe because `RETI` also pops PC — the next instruction is the one that was interrupted, and the program is in a consistent state.

## Summary Diagram

```
  Hardware Event (e.g., VBlank)
         |
         v
    IF register: bit set to 1  (request recorded)
         |
         v
    IE register: is this bit enabled?  ----NO----> Sits in IF, waiting
         |
        YES
         |
         v
    IME flag: is master switch on?  -------NO----> Sits in IF, waiting
         |                                          (but can wake HALT)
        YES
         |
         v
    CPU DISPATCH (5 M-cycles):
      1. Clear IME to 0
      2. Clear the IF bit
      3-4. Push current PC onto stack
      5. Load vector address into PC
         |
         v
    HANDLER CODE RUNS
      - push registers
      - do work
      - pop registers
      - RETI (pop PC + set IME=1)
         |
         v
    Normal execution resumes
```

## References

- [Pan Docs — Interrupts](https://gbdev.io/pandocs/Interrupts.html)
- [Pan Docs — Interrupt Sources](https://gbdev.io/pandocs/Interrupt_Sources.html)
- [Pan Docs — HALT](https://gbdev.io/pandocs/halt.html)
- [Tetris Disassembly](https://github.com/alexsteb/tetris_disassembly)
