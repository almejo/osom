# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OSOM is a Game Boy emulator written in Java. It emulates the Z80 CPU, GPU, and memory subsystem to run Game Boy ROMs. It boots the Game Boy BIOS and can run games (e.g., Tetris).

## Build Commands

```bash
./gradlew build          # Build the project
./gradlew test           # Run tests (JUnit 4)
./gradlew clean build    # Clean and rebuild
```

**Run the emulator:**
```bash
java -cp build/classes/java/main:build/libs/* com.almejo.osom.Main --rom <path/to/rom>
java -cp build/classes/java/main:build/libs/* com.almejo.osom.Main --rom <path/to/rom> --no-bios
```

## Tech Stack

- **Java 17** (source and target compatibility)
- **Gradle 7.6** (wrapper included)
- **Lombok** 1.18.26 (`@Getter`, `@Setter` annotations throughout)
- **Apache Commons CLI** 1.4 (command-line argument parsing)
- **JUnit 4.13.2** (testing)
- **Swing** for the LCD display

## Architecture

### Emulation Loop (Emulator.java)

The main loop runs at 60 FPS (4,194,304 cycles/second, ~69,905 cycles/frame):
1. `cpu.execute()` — fetch and execute the next opcode
2. `cpu.updateTimers(cycles)` — advance timer registers
3. `cpu.checkInterrupts()` — handle pending interrupts (V-Blank, LCD, Timer, Joypad)
4. `gpu.update(cycles)` — advance GPU state machine (H-Blank → V-Blank → Sprites → Graphics)
5. Throttle to 16ms per frame

### Key Components

- **Z80Cpu** (`cpu/Z80Cpu.java`) — CPU core with registers (AF, BC, DE, HL, PC, SP), opcode dispatch via HashMap, interrupt handling, and timer management
- **Operation** (`cpu/Operation.java`) — Abstract base class for all CPU instructions. Each opcode is a subclass (e.g., `OperationLD_A_n`, `OperationADD_HL_rr`). Operations define opcode, byte length, M-cycles, and T-cycles
- **MMU** (`memory/MMU.java`) — Memory Management Unit mapping the 16-bit address space (ROM, VRAM, Work RAM, I/O registers, etc.), handles BIOS bootstrapping and DMA transfers
- **GPU** (`gpu/GPU.java`) — Graphics processor managing LCD modes, scanline rendering, tile/sprite drawing, and V-Blank interrupt generation
- **LCDScreen** (`LCDScreen.java`) — Swing JPanel rendering the 160x144 pixel display at 2x scale

### CPU Operation Pattern

Each Z80 instruction is a dedicated class extending `Operation`. Naming convention: `Operation{INSTRUCTION}_{OPERANDS}` (e.g., `OperationLD_r_n`, `OperationBIT_b_r`). Some operations use parameterized base classes (like `OperationLD_r_n` taking a register argument) while others are fully specialized. All operations must correctly update CPU flags (Zero, Subtract, Half-Carry, Carry) and clock cycles.

### Memory Map

All memory access routes through MMU. Key regions: ROM (0x0000-0x7FFF), VRAM (0x8000-0x9FFF), Work RAM (0xC000-0xDFFF), I/O (0xFF00-0xFF7F), High RAM (0xFF80-0xFFFE).

## Code Conventions

- Uses tabs for indentation
- Lombok `@Getter`/`@Setter` for accessor methods — do not write manual getters/setters where Lombok is already used
- ROM files go in the `roms/` directory (gitignored)
- **All `if`/`else`/`for`/`while` blocks must use braces `{}`** — even for single-line bodies. No braceless control flow.
- **All new code must have tests** — every new method, class, or behavior change requires corresponding Spock tests unless there is a clear, documented reason not to (e.g., pure wiring code, trivial delegation)
