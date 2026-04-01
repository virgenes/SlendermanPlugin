# 🕯️ SlendermanPlugin

[![Version](https://img.shields.io/badge/version-1.5.0-red.svg)](https://github.com/virgenes/SlendermanPlugin)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.19--1.21.x-green.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> A professional, feature-rich Slender Man horror minigame for Paper/Spigot servers.  
> Collect 8 pages before the Slenderman catches you — if you dare.

---

## ✨ Features

| Category | Features |
|---|---|
| 🎮 **Gameplay** | 8-page collection, sanity system, noise mechanics, multiple arenas |
| 🧠 **Sanity** | Visual bar, panic effects, hallucinations, darkness drain |
| ⚔️ **Perks** | 9 survivor perks + 3 Slenderman perks, equippable in-game |
| 🎭 **Disguises** | 6 Slenderman skins (Enderman, Wither, Phantom, Ravager, Elder Guardian, Warden) |
| 📊 **Progression** | Complete Evolution system: XP, Levels, Skills, and Achievements |
| 💰 **Economy** | Built-in coin system, balance commands, shop |
| 🌍 **Multi-arena** | Unlimited simultaneous arenas with independent config |
| 🔌 **Integrations** | PlaceholderAPI, ProtocolLib, ViaVersion compatible |
| 🌐 **Languages** | English, Spanish, French, German, Portuguese, Chinese |
| 🛠️ **Admin tools** | Game logs, hot-reload, wizard setup, force-start |

---

## 📋 Requirements

- **Server:** Paper or Spigot 1.19 – 1.21.x
- **Java:** 17 or higher
- **Required:** [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (for disguise system)
- **Optional:** [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (for stats in scoreboards)
- **Optional:** [ViaVersion](https://www.spigotmc.org/resources/viaversion.19254/) (for multi-version support)

---

## 🚀 Installation

1. Download `SlendermanPlugin-1.5.0.jar`
2. Place it in your server's `plugins/` folder
3. Install [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
4. Start the server — config files are generated automatically
5. Set the lobby: `/sis setlobby`
6. Create your first arena: `/sis createarena <id>`
7. Configure it: `/sis editarena <id>`
8. Save and play: `/sis admin save <id>`

---

## 🎮 Commands & Permissions

### Player Commands
| Command | Alias | Permission | Description |
|---|---|---|---|
| `/sis join <arena>` | — | `SlendermanPlugin.player` | Join an arena |
| `/sis leave` | — | `SlendermanPlugin.player` | Leave current arena |
| `/sis balance [player]` | `/sis bal` | `slender.economy.balance` | View coin balance |
| `/sis baltop` | `/sis balancetop` | `slender.economy.balancetop` | Top 10 richest |
| `/sis pay <player> <amount>` | — | `slender.economy.pay` | Send coins |
| `/sis money` | `/sis eco` | `slender.economy.help` | Economy help |

### Admin Commands
| Command | Alias | Permission | Description |
|---|---|---|---|
| `/sis setlobby` | — | `SlendermanPlugin.admin` | Set lobby location |
| `/sis createarena <id>` | — | `SlendermanPlugin.admin` | Create a new arena |
| `/sis editarena <id>` | — | `SlendermanPlugin.admin` | Open arena editor |
| `/sis deletearena <id>` | — | `SlendermanPlugin.admin` | Delete an arena |
| `/sis admin <id>` | — | `SlendermanPlugin.admin` | Admin menu for arena |
| `/sis start <arena>` | — | `SlendermanPlugin.admin` | Force-start an arena |
| `/sis money give <player> <amount>` | — | `slender.economy.give` | Give coins |
| `/sis money take <player> <amount>` | — | `slender.economy.take` | Take coins |
| `/sis money set <player> <amount>` | — | `slender.economy.set` | Set balance |
| `/sis money reload` | `/sis money rl` | `slender.economy.reload` | Reload config |

---

## ⚙️ Configuration

The main config is at `plugins/SlendermanPlugin/config.yml`.  
Key sections:

```yaml
General:
  Language: en          # en, es, fr, de, pt, zh
  Update-Checker: true

GameSettings:
  Pages-To-Win: 8
  SlenderMan-Health: 40
  Starting-Countdown: 30
  Torch:
    Max-Uses: 3
    Cooldown-Seconds: 5
  TerrorRadius:
    Enabled: true
    Music-Radius: 7
    Nausea-Radius: 3
  Disguise:
    Enabled: true
    Default-Skin: ENDERMAN  # ENDERMAN, WITHER, PHANTOM, RAVAGER, ELDER_GUARDIAN, WARDEN
```

Full config with 80+ parameters is auto-generated on first run.

---

## 🧠 Systems

### Sanity System
Survivors have 0–100 sanity. It drains when:
- Looking at the Slenderman (within 15 blocks)
- Standing in darkness
- Near the Slenderman (within 7 blocks)

**Effects by level:**
| Sanity | Effects |
|---|---|
| 75–100 | Normal |
| 50–74 | Occasional ambient sounds |
| 25–49 | Nausea, slowness, blindness pulses |
| 0–24 | **PANIC** — severe effects, vulnerability |

### Evolution System (Leveling)
Players earn EXP by playing games, collecting pages, killing survivors, or killing Slenderman. 
As they level up, they unlock new levels with specific coin rewards configured in `levels.yml`.

### Skills System
Players can use their earned coins to upgrade permanent passive skills up to Level 5:
| Skill | Effect |
|---|---|
| **Walk Speed** | Increases base movement speed by +5% per level |
| **Stamina** | Increases sprint duration |
| **Resistance** | Reduces damage taken by monsters by -5% per level |
| **Coin Booster** | Increases coins earned per match |

### Achievements
Players have a dedicated tracker to unlock lifetime achievements based on total stats:
- **Scholar I-III**: Collect 8, 40, and 200 total pages
- **Hunter I-III**: Kill 5, 25, and 100 survivors as Slenderman
- **Survivor I-III**: Win 1, 10, and 50 matches as Survivor

### Survivor Perks
| Perk | Effect |
|---|---|
| Runaway | Speed II for 5s (20s cooldown) |
| Better Together | Regeneration for you + nearby ally (25s) |
| Archaeologist | Compass → nearest page + Night Vision (30s) |
| Iron Will | Resistance I for 8s (30s) |
| Shadow Step | Invisibility for 4s (35s) |
| Last Stand | Speed III + Strength I for 5s (45s) |
| Resilience | Passive 40% less sanity drain + restore 25 sanity (45s) |
| Tracker | Compass → nearest page for 10s (25s) |
| Spirit | Passive: on death, slows Slenderman 5s |

### Slenderman Perks
| Perk | Effect |
|---|---|
| Blood Hunt | Speed II + Slowness I on all survivors (40s) |
| Terrify | Nausea + Darkness on nearby survivors (30s) |
| Aura Sense | Passive: survivors glow 3s when picking a page |

---

## 📊 PlaceholderAPI

If PlaceholderAPI is installed, these placeholders are available:

| Placeholder | Description |
|---|---|
| `%slender_level%` | Player level |
| `%slender_rank%` | Player rank name |
| `%slender_coins%` | Coin balance |
| `%slender_wins%` | Total wins |
| `%slender_pages%` | Total pages collected |
| `%slender_deaths%` | Total deaths |
| `%slender_games%` | Total games played |
| `%slender_sanity%` | Current sanity (in-game) |
| `%slender_arena%` | Current arena ID |
| `%slender_role%` | Current role |

---

## 🗂️ File Structure

```
plugins/SlendermanPlugin/
├── config.yml          # Main configuration
├── langauge.yml        # Legacy language file (auto-migrated)
├── levels.yml          # XP/level configuration
├── lang/               # Language files
│   ├── en.yml
│   ├── es.yml
│   ├── fr.yml
│   ├── de.yml
│   ├── pt.yml
│   └── zh.yml
├── arenas/             # Arena configuration files
│   └── <arena-id>.yml
├── users/              # Player data files
│   └── <uuid>.yml
└── logs/               # Game logs
    └── YYYY-MM-DD.log
```

---

## 🔧 API

SlendermanPlugin fires custom events that other plugins can listen to:

```java
// Player gains EXP
@EventHandler
public void onExpGain(SlenderPlayerExpGainEvent event) {
    GamePlayer player = event.getGamePlayer();
    int exp = event.getExp();
}

// Player levels up
@EventHandler
public void onLevelUp(SlenderPlayerLevelUpEvent event) {
    int newLevel = event.getNewLevel();
}

// Game starts
@EventHandler
public void onGameStart(SlenderGameStartEvent event) {
    IArena arena = event.getArena();
}

// Game ends
@EventHandler
public void onGameEnd(SlenderGameEndEvent event) {
    IArena arena = event.getArena();
}
```

---

## ❓ FAQ

**Q: The Slenderman disguise doesn't work.**  
A: Make sure ProtocolLib is installed. Check console for `[Disguise] ProtocolLib disguise ready.`

**Q: Players get kicked with protocol errors.**  
A: This was a ViaVersion compatibility issue, fixed in 1.5.0 by using ProtocolLib for packet injection.

**Q: How do I create an arena?**  
A: Use `/sis createarena <id>`, then `/sis editarena <id>` to set spawns and pages, then `/sis admin save <id>`.

**Q: Can I use this on 1.16?**  
A: The plugin targets 1.19+. Some features (DARKNESS effect, WARDEN disguise) require 1.19+. The plugin adapts automatically on older versions.

---

## 📜 Changelog

### v1.5.0
- Complete rewrite and bug fixes
- Internal disguise system (no LibsDisguises required)
- Sanity system with visual bar
- 9 survivor perks + 3 Slenderman perks
- 6 Slenderman skins in shop
- Economy system with balance commands
- PlaceholderAPI integration
- 6-language support
- Game logging system
- Lantern with battery system
- In-game scoreboard with sanity/pages/time
- Custom chat format with rank badges
- ViaVersion compatibility via ProtocolLib

---

## 📄 License

MIT License — free to use, modify and distribute.  
Please credit **virgenes** if you redistribute.

---

## 🏷️ Credits

This plugin is based on the original version by **MrABCDevelopment**:  
🔗 [https://github.com/MrABCDevelopment/Slender](https://github.com/MrABCDevelopment/Slender)
