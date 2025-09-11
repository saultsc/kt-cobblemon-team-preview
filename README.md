# Cobblemon Team Preview

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.16.5-orange.svg)](https://fabricmc.net/)
[![Cobblemon](https://img.shields.io/badge/Cobblemon-1.6.1-purple.svg)](https://cobblemon.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-blue.svg)](https://kotlinlang.org/)

A Fabric mod for Cobblemon that adds team preview functionality to battles, allowing players to see their opponent's team and strategically select their starting Pokémon.

## ✨ Features

- **Team Preview Screen**: View your opponent's team before battle starts
- **Strategic Selection**: Choose your starting Pokémon based on the opponent's team
- **Timer System**: Built-in selection timer with pre-battle countdown
- **Network Synchronization**: Client-server packet handling for multiplayer battles
- **Singles Battle Support**: Currently optimized for 1v1 battles

## 🚀 Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.1
2. Download and install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download and install [Cobblemon](https://modrinth.com/mod/cobblemon) 1.6.1+
4. Download the latest release from [Releases](../../releases)
5. Place the mod file in your `mods` folder

## 🛠️ Building

```bash
./gradlew build
```

The built mod will be available in `build/libs/`

## 🎮 How it Works

When a battle starts:

1. Both players see a team preview screen showing their opponent's Pokémon
2. Players have 30 seconds to select their starting Pokémon
3. A 5-second countdown begins once both players have selected
4. The battle starts with the chosen Pokémon

## ⚙️ Technical Details

- **Language**: Kotlin
- **Framework**: Fabric
- **Minecraft Version**: 1.21.1
- **Dependencies**: Fabric API, Cobblemon
- **Architecture**: Client-server networking with custom packets

## 📝 Development Notes

### Mixins

When writing mixins, use Java instead of Kotlin due to limited Kotlin support in the Mixin framework.
See: [Sponge Issue #245](https://github.com/SpongePowered/Mixin/issues/245)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Contributing

Contributions are more than welcome! Please feel free to submit a Pull Request.
