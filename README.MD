<p align="center">
  <img src="https://cdn.c4vxl.de/Agora/logo-clear.png" alt="Bot Logo" style="width: 35ch">
</p>

<h2 align="center">Agora – A Fair & Feature-Rich Discord Bot</h2>

<p align="center">
  <b>Agora</b> - a modular Discord bot for organizing communities, conversations, and shared activities.
</p>

<br>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue" />
  <img src="https://img.shields.io/badge/Platform-Discord-lightgrey" />
  <img src="https://img.shields.io/badge/Type-Bot-green" />
</p>

---

<br>

## Table of Contents
- [What is Agora?](#what-is-agora)
- [Example Features](#example-features)
    - [Activities & Interests](#activities--interests)
    - [Custom Channels](#custom-channels)
    - [Ticket System](#ticket-system)
- [Architecture & Design](#architecture--design)
    - [Where to find what](#where-to-find-what)
- [Configuration](#configuration)
- [Installation](#installation)
- [Build yourself](#build-yourself)
- [License](#license)

<br>

## What is Agora?
**Agora** is a Discord bot focused on fairness, automation, and ease of use.

Agora takes care of standard community features. It is built around the concept of **isolated features**, each responsible for a specific part of the
server experience (activities, tickets, channels, rules, etc.).  
<br>

## Example Features

#### Activities & Interests
Users can subscribe to activities they are interested in and receive notifications when someone else starts them.

- Interest management via commands or components
- Start activities with optional messages
- Ignore activities you’re not interested in

<br>

### Custom Channels
Enables users to create their own voice or text channels dynamically.

- Button or command-based channel creation
- Automatic channel handling and cleanup
- Fully configurable behavior

<br>

### Ticket System
A structured ticket system for support, feedback, or general requests.

- Create tickets via button interaction or command
- Requires a title and detailed description

<br>

And much more...
<br>

## Architecture & Design
The bot follows a **clean, modular architecture**:

- Each feature is encapsulated in its own class
- Centralized command, data and component handling
- Shared utilities for permissions, embeds, logging, and resources
- Clear separation between logic, data, and configuration

<br>

#### Where to find what:
*Here are some direct links to the different sections of the Agora code base:*
| Component | Description |
| --------- | ----------- |
| [**/bot/feature/**](https://github.com/c4vxl/Agora/tree/main/src/main/kotlin/bot/feature/) | All guild-specific features of the bot. |
| [**/bot/handler/**](https://github.com/c4vxl/Agora/tree/main/src/main/kotlin/bot/handler/) | Centralized handling of guild-specific data, commands, components, etc. |
| [**/data/**](https://github.com/c4vxl/Agora/tree/main/src/main/kotlin/data/) | Database, caching and guild-specific data structures. |
| [**/config/**](https://github.com/c4vxl/Agora/tree/main/src/main/kotlin/config/) | Configuration loader and interface. |
| [**/language/**](https://github.com/c4vxl/Agora/tree/main/src/main/kotlin/language/) | Multi-language support and localization logic. |
| [**/utils/**](https://github.com/c4vxl/Agora/tree/main/src/main/kotlin/utils/) | Shared utilities for embeds, permissions, channels, logging and more. |
| [**/enums/**](https://github.com/c4vxl/Agora/tree/main/src/main/kotlin/config/enums/) | Global constants such as colors, embeds or permissions. |

<br>

## Configuration
**Agora** uses a single `config.json` file that will be created in the root directory as the bot starts for the first time.

Below is a breakdown of each option:
| Key | Description |
|-----|-------------|
| `token` | _Your Discord bot token. Replace **<paste_your_token_here>** with your bot's token. Do not share this publicly._ |
| `db_path` | _Path to the folder where the bot stores its database files. **Default: ./db/**._ |
| `db_cache_save_interval` | _Interval in milliseconds for saving the in-memory database cache to disk. **Default: 30000 (30 seconds)**._ |
| `db_pretty_print` | _If true, database files are saved in a human-readable format (JSON pretty-printed). **Default: false**._ |
| `lang_dir` | _Path to the folder where the translation files are stored. **Default: ./langs/**._ |
| `activity` | _The bot’s Discord activity message (what it “is doing”). **Default: "Just vibing"**._ |
| `status` | _The bot’s Discord status. Options include `online`, `idle`, `dnd`, `invisible`. **Default: idle**._ |
| `only_join_whitelisted` | _If true, the bot will only join guilds listed in whitelist. **Default: false**._ |
| `whitelist` | _List of Discord server IDs where the bot is allowed to join if `only_join_whitelisted` is enabled. **Default: []**._ |
| `disable_features` | _List of feature names to disable globally on startup (e.g., ["ActivityFeature", "TicketFeature"]). **Default: []**._ |

<br>

# Installation
> [!NOTE]
> Agora was build using a JDK of version 21.

<br>

Here's a step by step guide to installing **Agora**:

1. Download the `Agora-{version}.jar` from the [[latest release]](https://github.com/c4vxl/Agora/releases/latest) in this repository.
2. Execute it using Java 21: `java -jar Agora-*.jar`.

<br>

## Build yourself
To compile **Agora** youself, simply follow these steps:
1. Clone this repository
2. Build it using gradle: `./gradlew clean build`
3. The compiled `.jar` file can be found at /build/libs/

<br>

# License
You are welcome to use and experiment with this project freely for personal purposes.

Note that:
- If you use Agora on your own Server, you must credit the original project / author (c4vxl).
- For any larger-scale or commercial use, or if you plan to redistribute or modify Agora, you are required contact the author first to discuss terms.

This bot is provided as-is, without warranties, and is not optimized for high-performance production environments.

---

A project by [c4vxl](https://info.c4vxl.de/)