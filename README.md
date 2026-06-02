# Presentation Mod

Presentation Mod is a Minecraft Fabric mod that allows players to create and display presentations directly inside the game world.

The mod adds modular presentation screen blocks, an in-game upload interface, and a remote controller item for switching slides. It is designed for education, demonstrations, roleplay servers, events, lessons, project showcases, and any situation where players need to present information inside Minecraft.

## Features

- Build large in-world presentation screens from multiple `Presentation Screen` blocks.
- Upload presentation files directly through an in-game GUI.
- Display slides inside the Minecraft world.
- Control slides using the `Presentation Remote` item.
- Synchronize the selected presentation and current slide between server and nearby clients.
- Preserve screen state after world reloads.
- Load and cache rendered slides on both the server and client.
- Automatically select visual screen parts: corners, edges, middle, and single block.
- Disable a screen with sneak right-click; disabled screens stop rendering slides and have no collision.

## Supported File Formats

Presentation Mod supports:

- PDF
- PPTX
- PNG
- JPG / JPEG
- WEBP
- BMP
- GIF
- Folders with image slides sorted by file name

Uploaded files are stored in:

```text
config/presentationmod/presentations
```

## How To Use

1. Place one or more `Presentation Screen` blocks in the world.
2. Right-click the screen to open the upload interface.
3. Select a PDF, PPTX, image file, or a folder with image slides.
4. Bind the `Presentation Remote` to the screen by right-clicking the screen with the remote.
5. Use the remote to move between slides.
6. Sneak-use the remote to go to the previous slide.

## Controls

- Right-click a screen block: open the presentation upload screen.
- Shift + right-click any block of a connected screen: enable or disable that whole screen.
- Right-click with a bound `Presentation Remote`: next slide.
- Shift-use the `Presentation Remote`: previous slide.

When a screen is disabled, its slide renderer is skipped and its collision shape is empty.

## Screen Layout Rules

All placed blocks are the same item: `presentationmod:presentation_screen`. The mod assigns the visible model automatically.

A connected presentation screen is created only when blocks form a complete rectangle with both width and height at least `2`.

Examples:

```text
2x2, 3x2, 3x3 -> one connected presentation screen
1x3, 3x1      -> separate single-block screens
L-shape       -> separate single-block screens
```

For a `3x3` connected screen, parts are assigned like this:

```text
left_up        up        right_up
left           middle    right
left_down      down      right_down
```

`presentation_screen` is used only for a single `1x1` screen.

## Requirements

- Minecraft 1.19.2
- Fabric Loader 0.16.14+
- Fabric API 0.77.0+1.19.2
- Fabric Language Kotlin 1.10.18+kotlin.1.9.22
- Java 17

## Recommended Use Cases

This mod is useful for:

- School and university projects.
- Educational Minecraft worlds.
- Server events.
- In-game lectures.
- Presentations on roleplay servers.
- Project demonstrations.
- Tutorials and information boards.

## Notes

This mod is made for in-game presentation and educational purposes. It does not connect to external online presentation services. Files are uploaded and stored locally on the server in the mod configuration folder.

## Build

Use the bundled Gradle wrapper:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The compiled mod jar will be created in:

```text
build/libs
```

Use this jar in a normal `mods` folder:

```text
build/libs/presentationmod-2.0.jar
```

Other jars such as `*-dev.jar` or `*-dev-shadow.jar` are development/build artifacts and are not the normal mod jar.

## Development

Run a local client:

```bash
./gradlew runClient
```

Run a local server:

```bash
./gradlew runServer
```

## Test Commands

Give the screen and remote:

```mcfunction
/give @p presentationmod:presentation_screen
/give @p presentationmod:presentation_remote
```

Place a specific visual part for model testing:

```mcfunction
/setblock ~ ~ ~ presentationmod:presentation_screen[part=left_up,facing=south,active=true]
```

Available `part` values:

```text
single
middle
left
right
up
down
left_up
left_down
right_up
right_down
```

## Project Structure

```text
src/main/kotlin/com/example/presentationmod
|-- block       # Presentation screen block and block entity
|-- client      # GUI, rendering, texture cache, client events
|-- item        # Presentation remote item
|-- network     # Upload, sync, and slide image packets
|-- registry    # Blocks, items, block entities, creative tab
`-- util        # File loading, screen bounds, server/client helpers
```

## License

MIT
