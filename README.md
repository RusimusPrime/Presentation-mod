# Presentation Mod

Presentation Mod is a Minecraft Forge mod for in-game presentations. It adds a modular presentation screen block, a remote controller, and a file upload flow for showing slides directly inside the world.

The mod is built for Minecraft 1.20.1 on Forge 47.3.0 and uses Kotlin for Forge.

## Features

- In-world presentation screens assembled from multiple `Presentation Screen` blocks.
- Upload presentations from the client through an in-game GUI.
- Render PDF, PPTX, image files, or folders with image slides.
- Synchronize selected presentation and current slide between server and nearby clients.
- Control slides with the `Presentation Remote` item.
- Preserve screen state in block entity NBT.
- Load and cache rendered slides on both the server and client.

## Supported Formats

- PDF
- PPTX
- PNG, JPG, JPEG, WEBP, BMP, GIF
- Folders containing image slides sorted by file name

Uploaded files are stored under:

```text
config/presentationmod/presentations
```

## Requirements

- Java 17
- Minecraft 1.20.1
- Forge 47.3.0+
- Kotlin for Forge 4.x

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

## Development

Run a local client:

```bash
./gradlew runClient
```

Run a local server:

```bash
./gradlew runServer
```

## How To Use

1. Place one or more `Presentation Screen` blocks in the world.
2. Right-click the screen to open the upload interface.
3. Select a PDF, PPTX, image, or slide folder source.
4. The server stores the file and syncs the first slide to nearby clients.
5. Bind a `Presentation Remote` to the screen by right-clicking the screen with the remote.
6. Use the remote to switch slides. Sneak-use moves to the previous slide.

## Project Structure

```text
src/main/kotlin/com/example/presentationmod
├── block       # Presentation screen block and block entity
├── client      # GUI, rendering, texture cache, client events
├── item        # Presentation remote item
├── network     # Upload, sync, and slide image packets
├── registry    # Blocks, items, block entities, creative tab
└── util        # File loading, screen bounds, server/client helpers
```

## License

MIT
