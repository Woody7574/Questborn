# Questborn

An advanced questing and story engine plugin for Minecraft (Paper, Spigot, Purpur, Folia).

## Requirements

- **Java**: 16 or newer (Java 17/21 recommended)
- **Server**: Paper, Spigot, Purpur, or Folia (1.16+)

## Building from Source

To compile and package the plugin using the included Gradle wrapper:

### Linux / macOS:
```bash
./gradlew build
```

### Windows:
```cmd
gradlew.bat build
```

The compiled jar file with shadowed dependencies will be located in:
`build/libs/Questborn-2.0.4.jar`

## Features

- **Multi-thread & Folia Support**: High performance asynchronous database and region handling.
- **Dynamic Quests**: Branching dialogue trees, interactive NPC dialogues, conditions, and custom objectives.
- **Rich Integrations**: Citizens, FancyNpcs, ItemsAdder, CraftEngine, PlaceholderAPI, Vault, WorldGuard.
- **Multi-language**: Built-in support for multiple languages with in-game configuration.
- **In-Game GUI Editors**: Comprehensive graphical editors for quests, rewards, and dialogue chains.
