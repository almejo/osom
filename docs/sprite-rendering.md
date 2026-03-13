# Sprite Rendering (OBJ)

This document explains how the Game Boy renders sprites (also called "objects" or OBJ in official documentation). It covers the OAM memory layout, attribute flags, tile addressing, the per-scanline rendering pipeline, and how sprites interact with the background layer.

A developer reading this document should understand Game Boy sprite hardware *before* looking at the source code in `GPU.java`.

## OAM Memory Layout

Sprite data lives in **Object Attribute Memory (OAM)**, a 160-byte region at `0xFE00-0xFE9F`. OAM holds 40 sprite entries, each 4 bytes:

| Byte | Offset | Name       | Description |
|------|--------|------------|-------------|
| 0    | +0     | Y Position | Sprite's vertical position + 16. A value of 16 places the sprite at screen Y=0. A value of 0 hides the sprite above the screen. |
| 1    | +1     | X Position | Sprite's horizontal position + 8. A value of 8 places the sprite at screen X=0. A value of 0 hides the sprite off the left edge. |
| 2    | +2     | Tile Index | Index into tile data at `0x8000`. In 8x16 mode, the LSB is forced (see below). |
| 3    | +3     | Attributes | Flags controlling rendering behavior (see Attribute Byte section). |

The Y and X offsets (16 and 8 respectively) exist so that sprites can be partially scrolled off the top or left edges of the screen. A sprite at Y=16, X=8 appears at screen position (0, 0). A sprite at Y=0, X=0 is completely off-screen.

> **Pan Docs reference:** [OAM — Object Attribute Memory](https://gbdev.io/pandocs/OAM.html)

## Sprite Attribute Byte

Byte 3 of each OAM entry is the attribute/flags byte. On the original DMG (non-Color Game Boy), bits 0-3 are unused:

| Bit | Name        | Description |
|-----|-------------|-------------|
| 7   | BG Priority | `0` = sprite renders on top of background; `1` = background colors 1-3 render over the sprite (color 0 is always behind sprites) |
| 6   | Y-Flip      | `0` = normal; `1` = sprite is vertically mirrored |
| 5   | X-Flip      | `0` = normal; `1` = sprite is horizontally mirrored |
| 4   | DMG Palette | `0` = use OBP0 (`0xFF48`); `1` = use OBP1 (`0xFF49`) |
| 3-0 | CGB Only    | Used on Game Boy Color for VRAM bank and palette selection; ignored on DMG |

> **Pan Docs reference:** [OAM — Object attributes](https://gbdev.io/pandocs/OAM.html#object-attributes)

## Tile Data Addressing

Sprites **always** use the unsigned tile addressing mode, reading tile data from `0x8000-0x8FFF`. This differs from the background, which can use either `0x8000` (unsigned) or `0x8800` (signed) addressing depending on LCDC bit 4.

The address formula for a sprite tile row:

```
tileDataAddress = 0x8000 + tileIndex * 16 + tileRow * 2
```

Each tile is 16 bytes (8 rows, 2 bytes per row). The two bytes for each row encode four possible color indices (0-3) using a planar format: bit N of byte 1 provides the low bit of pixel N's color, and bit N of byte 2 provides the high bit.

**Why sprites always use `0x8000`:** The background's signed addressing mode (`0x8800` base) was designed to let tile indices 0-127 map to `0x8800-0x8FFF` and signed indices -128 to -1 map to `0x8000-0x87FF`, giving shared access to the same tile memory. Sprites don't need this flexibility — they use a simple unsigned index into the `0x8000` region.

> **Pan Docs reference:** [Tile Data](https://gbdev.io/pandocs/Tile_Data.html)

## Per-Scanline Rendering Pipeline

The GPU renders sprites during the `drawLine()` method, called once per visible scanline (lines 0-143) during the Mode 3 (GRAPHICS) phase. The rendering follows this pipeline:

### 1. Determine Sprite Height

LCDC bit 2 controls sprite size:
- **Bit 2 = 0:** 8x8 pixel sprites (one tile per sprite)
- **Bit 2 = 1:** 8x16 pixel sprites (two tiles stacked vertically)

### 2. Scan All 40 OAM Entries

For each OAM entry (in order from entry 0 to entry 39):

1. Read the 4-byte entry (Y, X, tile index, attributes)
2. Compute the sprite's screen Y: `spriteY = oamY - 16`
3. Check if the current scanline falls within the sprite's vertical range: `line >= spriteY && line < spriteY + spriteHeight`
4. If the sprite is not on this scanline, skip to the next entry
5. Increment the per-scanline sprite counter

### 3. Enforce 10-Sprite-Per-Scanline Limit

The Game Boy hardware can only render 10 sprites per scanline. Once 10 sprites have been found on the current scanline, remaining OAM entries are ignored (even if they overlap the line). This limit is based on OAM position order — sprite entry 0 has highest priority.

### 4. Compute Tile Row

The row within the tile to render: `tileRow = line - spriteY`

If **Y-flip** (attribute bit 6) is set: `tileRow = spriteHeight - 1 - tileRow`

### 5. Handle 8x16 Mode Tile Index

In 8x16 mode, the tile index from OAM has its least significant bit (LSB) forced:
- **Top tile** (rows 0-7): `tileIndex & 0xFE` (LSB cleared to 0)
- **Bottom tile** (rows 8-15): `tileIndex | 0x01` (LSB set to 1)

This means adjacent tile pairs are always used together. If OAM specifies tile index 0x03, the top half uses tile 0x02 and the bottom half uses tile 0x03.

### 6. Read Tile Data and Render Pixels

For each of the 8 pixel columns (0-7):

1. Compute screen X: `screenX = oamX - 8 + column`
2. If `screenX < 0` or `screenX >= 160`, skip (off-screen clipping)
3. Compute bit position in tile bytes:
   - Normal: `bit = 7 - column`
   - X-flip (attribute bit 5): `bit = column`
4. Extract 2-bit color index from tile byte pair (same planar format as background tiles)
5. If color index is 0, skip — **color index 0 is always transparent for sprites**
6. If BG priority (attribute bit 7) is set and the background color index at this pixel is non-zero, skip — the background takes priority
7. Look up the palette:
   - Attribute bit 4 = 0: use OBP0 (`0xFF48`)
   - Attribute bit 4 = 1: use OBP1 (`0xFF49`)
8. Compute shade: `shade = (palette >> (colorIndex * 2)) & 0x03`
9. Write the pixel to the frame buffer

> **Pan Docs reference:** [Rendering — Object rendering](https://gbdev.io/pandocs/Rendering.html)

## Transparency Rules

Sprite transparency uses a simple rule: **color index 0 is always transparent**, regardless of the palette mapping. This differs from the background, where color index 0 is drawn (it represents the "background color").

This means if a sprite tile has some pixels with color index 0, those pixels will show whatever is behind them (background pixels or nothing). This is how sprites can have non-rectangular visible shapes.

## Sprite-Background Priority (BG Priority Bit)

The BG priority flag (attribute bit 7) controls how sprites interact with the background layer:

- **Bit 7 = 0 (default):** The sprite renders on top of the background. Sprite pixels (with non-zero color index) always overwrite background pixels.
- **Bit 7 = 1:** Background colors 1, 2, and 3 render over the sprite. Only where the background color index is 0 does the sprite show through.

To implement this, the GPU stores the raw background color indices (before palette lookup) in a per-scanline array during `renderBackground()`. When `renderSprites()` encounters a sprite with BG priority set, it checks this array — if the background color index at that pixel position is non-zero, the sprite pixel is skipped.

> **Pan Docs reference:** [OAM — BG-to-OAM Priority](https://gbdev.io/pandocs/OAM.html)

## Palette Registers

Sprites use two separate palette registers, selectable per-sprite via attribute bit 4:

| Register | Address  | Name | Description |
|----------|----------|------|-------------|
| OBP0     | `0xFF48` | Object Palette 0 | Default sprite palette (attribute bit 4 = 0) |
| OBP1     | `0xFF49` | Object Palette 1 | Alternate sprite palette (attribute bit 4 = 1) |

Both palettes use the same 2-bit-per-color format as BGP (`0xFF47`):

```
Bits 7-6: Color for index 3
Bits 5-4: Color for index 2
Bits 3-2: Color for index 1
Bits 1-0: Color for index 0 (irrelevant for sprites — index 0 is always transparent)
```

The shade lookup formula: `shade = (palette >> (colorIndex * 2)) & 0x03`

Having two separate palettes allows different sprites to use different color mappings. For example, in a game with a player character and enemies, the player might use OBP0 while enemies use OBP1 to visually distinguish them.

> **Pan Docs reference:** [Palettes](https://gbdev.io/pandocs/Palettes.html)

## DMA Transfers

The Game Boy provides a hardware DMA (Direct Memory Access) mechanism for efficiently copying sprite data into OAM. Writing a value to the DMA register (`0xFF46`) triggers a transfer:

- **Source address:** `value << 8` (the written value multiplied by 256)
- **Destination:** `0xFE00-0xFE9F` (OAM)
- **Size:** 160 bytes (the entire OAM)

For example, writing `0xC0` to `0xFF46` copies 160 bytes from `0xC000-0xC09F` to `0xFE00-0xFE9F`.

Games typically maintain a shadow copy of OAM in work RAM and trigger DMA once per frame (usually during V-Blank) to update all sprite positions and attributes in a single operation. This is faster than writing each OAM byte individually.

On real hardware, during DMA only High RAM (`0xFF80-0xFFFE`) is accessible — the CPU cannot read from other memory regions. OSOM does not currently enforce this restriction (DMA executes instantly and does not block memory access).

> **Pan Docs reference:** [OAM DMA transfer](https://gbdev.io/pandocs/OAM_DMA_Transfer.html)

## 8x8 vs 8x16 Mode

LCDC bit 2 selects between two sprite sizes:

### 8x8 Mode (Bit 2 = 0)
Each sprite is one 8x8 tile. The tile index from OAM directly addresses a tile at `0x8000 + tileIndex * 16`. This is the simpler and more common mode.

### 8x16 Mode (Bit 2 = 1)
Each sprite is two 8x8 tiles stacked vertically (8 pixels wide, 16 pixels tall). The tile index from OAM has its LSB forced:

```
Top tile index:    tileIndex & 0xFE  (bit 0 cleared)
Bottom tile index: tileIndex | 0x01  (bit 0 set)
```

This means tile pairs always work together: tiles 0+1, 2+3, 4+5, etc. If OAM specifies tile index 5, the top half uses tile 4 and the bottom half uses tile 5.

The 8x16 mode setting is **global** — all 40 sprites use the same size. Games cannot mix 8x8 and 8x16 sprites in the same frame. Note that the Y offset (16 pixels) conveniently matches the 8x16 sprite height, allowing a sprite at Y=0 to be fully hidden above the screen in either mode.

> **Pan Docs reference:** [LCDC — OBJ size](https://gbdev.io/pandocs/LCDC.html#lcdc2--obj-size)

## Rendering Order and Overlap

When multiple sprites overlap at the same pixel position, the sprite with the **lowest OAM entry index** takes priority on the original DMG. This is because the GPU scans OAM entries in order (0 to 39) and the first sprite to write a non-transparent pixel at a given position "wins" — subsequent sprites at the same position are skipped because their pixels would overwrite (but in OSOM's implementation, later sprites *do* overwrite earlier ones at the same position since we don't track per-pixel sprite ownership). For correct DMG behavior, sprites with lower OAM indices should have higher priority.

> **Note:** The Game Boy Color changes this priority rule to use X-coordinate ordering instead of OAM position. OSOM targets DMG behavior only.
