# 🕯️ SlendermanPlugin

[![Version](https://img.shields.io/badge/version-1.7.0-red.svg)](https://github.com/virgenes/SlendermanPlugin)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.19--1.21.x-green.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> A professional, feature-rich Slender Man horror minigame for Paper/Spigot servers.  
> Featuring multiple game modes, a custom music engine, and high-performance anti-cheat.

---

## ✨ Features

| Category | Features |
|---|---|
| 🎮 **Game Modes** | **Classic** (Pages), **Escape Room** (Objectives), **Infection** (Alpha Slender) |
| 🛡️ **Anti-Cheat** | **Combat-Logout System** (Disconnected survivors after hit are penalized) |
| 🎵 **Audio** | **Independent NBS Engine** (Initial & Combat tracks, per-player toggle) |
| 🌐 **Localization** | **Advanced System**: Dynamic `lang/` folder, automatic extraction |
| 🧠 **Sanity** | Visual bar, panic effects, hallucinations, darkness drain |
| 🎭 **Disguises** | 6 Slenderman skins (Enderman, Wither, Phantom, Ravager, etc.) |
| 🔌 **Integrations** | PlaceholderAPI, ProtocolLib, ViaVersion compatible |

---

## 📋 Requirements

- **Server:** Paper or Spigot 1.16 – 1.21.4
- **Java:** 17 or higher
- **Required:** [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (for disguise & anti-cheat)

---

## 🚀 Installation

1. Download `SlendermanPlugin-1.7.0.jar`
2. Place it in your server's `plugins/` folder
3. Install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
4. Start the server — config and `lang/` are generated automatically
5. Set the lobby: `/sis setlobby`
6. Create your first arena: `/sis createarena <id>`
7. Configure it: `/sis editarena <id>` (Select your preferred **Game Mode**)
8. Save and play: `/sis admin save <id>`

---

## 🏗️ Game Modes

### 📖 Classic Mode
The traditional experience. Survivors must find and collect 8 scattered pages while keeping their sanity high and avoiding the Slenderman.

### 🔌 Escape Room Mode
A strategic team-based mode:
- **Repair Generators**: Find and fix 5 generators to power the exit.
- **Find Master Keys**: Scavenge for hidden keys to unlock the final gate.
- **Keypad Security**: Enter the numeric code to open the iron door.

### 🧟 Infection Mode
A rapid-fire survival mode where the infection spreads:
- **First Slender**: One player starts as the Alpha Slender.
- **Assimilation**: Killed survivors become **Proxies** and join the hunt.
- **Last Hope**: If only one survivor remains, they receive temporary Speed, Resistance, and Glowing buffs to make a final stand.

---

## 🛡️ Combat-Logout Anti-Cheat
Version 1.7.0 introduces a robust anti-cheat to ensure fairness:
- **Combat Tracking**: Players are in "combat" for 10s after being hit by SlenderMan.
- **Instant Loss Detection**: If the last survivor combat-logs, SlenderMan wins **immediately**.
- **Cheater Relegation**: Disconnecting in combat marks you as a cheater; upon return, you are forced into **Spectator mode** with no items or survivor effects.

---

## 🌍 Localization
Dynamic language loading system:
- **Automatic Extraction**: Languages (`en.yml`, `es.yml`, etc.) extract to `plugins/SlendermanPlugin/lang/`.
- **Full UI Translation**: Every menu, item, and message is localized.

---

## 📜 Changelog

### v1.7.0 (Latest)
- **⚡ Combat-Logout Anti-Cheat**: Resolved match-end hang bugs and enforced spectator restoration rules.
- **🧟 Infection Mode**: Fully implemented role transitions and survivor buffs.
- **🔓 Escape Room Mode**: Added generators, keypads, and master keys.
- **🌐 Professional Localization**: New dynamic `lang/` directory loading.
- **🚪 Iron Door & Interaction Fixes**: Synchronized door behavior and empty-hand interaction.
- **🎭 Disguise System Overhaul**: Real-time virtual packet injection via ProtocolLib.

---

## 📄 License
MIT License. Please credit **virgenes** if you redistribute.

---

## 🏷️ Credits

This plugin is based on the original version by **MrABCDevelopment**:  
🔗 [https://github.com/MrABCDevelopment/Slender](https://github.com/MrABCDevelopment/Slender)
