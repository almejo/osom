# OSOM Code Style Guide

This document describes the coding conventions for the OSOM Game Boy emulator. All contributors should follow these rules to maintain consistency across the codebase.

## Indentation

Use **tabs** for indentation in all Java and Groovy files. Do not use spaces for indentation.

## Naming

### Variable and Parameter Names

Use complete, descriptive names. No abbreviations.

```java
// Good
catch (RuntimeException exception) {
    log.error("Failed to load ROM", exception);
}

// Bad
catch (RuntimeException e) {
    log.error("Failed to load ROM", e);
}
```

**Exception:** Game Boy hardware terminology is allowed as-is, since these are domain-standard names documented in the Pan Docs:

- **Register names:** PC, SP, AF, BC, DE, HL
- **Subsystem acronyms:** MMU, GPU, ALU, DMA, OAM, APU
- **Register mnemonics:** TIMA, LCDC, SCX, SCY, TAC, TMA, IF, IE

### Class Naming for CPU Operations

Each CPU instruction is a dedicated class extending `Operation`. Naming convention:

```
Operation{INSTRUCTION}_{OPERANDS}
```

Examples: `OperationLD_A_n`, `OperationADD_HL_DE`, `OperationBIT_7_H`

When a new opcode belongs to an existing parameterized family (e.g., `LD r,r`, `INC r`, `BIT b,r`), register a new instance of the existing base class rather than creating a new class.

## Imports

### No Wildcard Imports

Always use explicit imports. Never use `import foo.*`.

```java
// Good
import java.util.HashMap;
import java.util.List;

// Bad
import java.util.*;
```

### No Fully-Qualified Class Names Inline

Always import the class and use the short name in code.

```java
// Good
import org.slf4j.Logger;
Logger logger = ...;

// Bad
org.slf4j.Logger logger = ...;
```

## Fields

### Prefer `final`

If a field can be declared `final`, it must be. This applies to all classes.

```java
// Good
private final int[][] pixels = new int[160][144];
private final HashMap<Integer, Operation> operations = new HashMap<>();

// Bad (if the field is never reassigned)
private int[][] pixels = new int[160][144];
private HashMap<Integer, Operation> operations = new HashMap<>();
```

### Lombok Accessors

Use Lombok `@Getter` and `@Setter` annotations for accessor methods. Do not write manual getters or setters where Lombok is already used on the class.

```java
// Good — let Lombok generate it
@Getter
@Setter
private MMU mmu;

// Bad — manual getter when Lombok is available
private MMU mmu;
public MMU getMmu() { return mmu; }
```

## Constants

### Named Constants for I/O Registers

All memory-mapped I/O addresses must use named constants. No magic hex numbers for register addresses. Constants belong in the class that owns the register (e.g., interrupt constants in `MMU`, LCD constants in `GPU`, timer constants in `Z80Cpu`).

```java
// Good
public static final int LCD_CONTROLLER = 0xFF40;
public static final int LCD_LINE_COUNTER = 0xFF44;
mmu.getByte(LCD_LINE_COUNTER);

// Bad
mmu.getByte(0xFF44);
```

### Static Final for True Constants

Immutable constants must be `static final`. This includes interrupt addresses, bit positions, memory region boundaries, and cycle counts.

```java
// Good
private static final byte FLAG_ZERO = 7;
private static final int PREFIX_CB = 0xcb;

// Bad
private static byte FLAG_ZERO = 7;  // missing final
```

## Logging

### SLF4J via Lombok `@Slf4j`

All classes that produce log output must use the Lombok `@Slf4j` annotation. No `System.out.println` anywhere in the codebase.

```java
// Good
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Z80Cpu {
    public void execute() {
        log.debug("Executing opcode 0x{}", Integer.toHexString(opcode));
    }
}

// Bad
public class Z80Cpu {
    public void execute() {
        System.out.println("Executing opcode " + opcode);
    }
}
```

**Log levels:**
- **TRACE** — CPU instruction execution details (registers, flags, opcode, cycles)
- **DEBUG** — Interrupt events, GPU mode transitions, timer updates
- **INFO** — Frame statistics (FPS, total cycles per frame)
- **WARN** — Unimplemented opcodes, unexpected states

## Architecture Boundaries

### No Swing/AWT in Core Packages

The packages `cpu/`, `memory/`, `gpu/`, and `input/` must have zero `javax.swing` or `java.awt` imports. Presentation code lives exclusively in the `ui/` package.

This separation enables headless testing and keeps the emulation core reusable.

## CPU Operation Comments

### Pan Docs Flag Shorthand

Every `Operation*` class that modifies CPU flags must include a flag shorthand comment documenting the expected flag behavior:

```java
// Flags: Z=*, N=0, H=*, C=-
```

Where:
- `*` = flag is affected (set or reset based on result)
- `0` = flag is always reset
- `1` = flag is always set
- `-` = flag is not affected

Example:
```java
public class OperationADD_A extends Operation {
    // Flags: Z=*, N=0, H=*, C=*
    @Override
    public void execute() { ... }
}
```

## Testing

### Test File Naming

All test files use the Spock framework and follow the naming convention `*Spec.groovy`, placed in `src/test/groovy/` mirroring the main source package structure.

```
src/test/groovy/com/almejo/osom/gpu/GPUInstanceIsolationSpec.groovy
src/test/groovy/com/almejo/osom/cpu/CpuInstanceIsolationSpec.groovy
```

### Test Discovery

Tests are discovered via `useJUnitPlatform()` in `build.gradle`. New `*Spec.groovy` files are automatically picked up.

### Test Logging

Test logging is configured at WARN level via `src/test/resources/logback-test.xml` to prevent TRACE output from flooding test runs.

## Build Journal

Non-trivial debugging sessions, opcode fixes, or GPU/timer/interrupt behavior changes should be recorded in `docs/build-journal.md` following the Pattern 7 format. See that file for the entry format and existing entries.
