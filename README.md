# Item Finder

A lightweight client mod for Minecraft 26.2 / Fabric by **MineAura**.

## What it does

- Press **G** (rebindable in Options → Controls → Item Finder) to open the search screen.
- Search by item display name or registry ID fragment, e.g. `netherite`, `diamond`, or `minecraft:iron_ingot`.
- Scan a configurable radius from 10 to 50 blocks.
- Search nearby **loaded** chests, barrels, and placed shulker boxes.
- Show each matching container, total matching item count, and exact coordinates.
- Select a result to show a purple translucent container glow through walls for six seconds, with a short ring of end-rod particles.
- If nothing is found, display: “Can't detect the item. Go to a different area and try again.”

## Important limitations

- This first version scans **single-player integrated-server worlds**. A client-only mod cannot inspect private container inventories on remote multiplayer servers; server-side support would be required there.
- Only already-loaded chunks are scanned. The mod does not force-load chunks.
- It scans placed chests, barrels, and shulker boxes in the current dimension. It does not scan player inventory, ender chests, entities, or shulker boxes carried as items.
- Radius is measured in 3D distance from the player's current block position.

## Build on Windows

Requirements: JDK 25 and Gradle 9.5.1 or newer on PATH.

Open PowerShell in this project folder and run:

```powershell
.\BUILD-WINDOWS.ps1
```

The compiled mod JAR is copied to `JAR/`.

## Mod identity

- Name: Item Finder
- Author: MineAura
- Mod ID: `itemfinder`
- License: MIT
