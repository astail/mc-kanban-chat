# KanbanChat

A Paper plugin that shows the contents of a designated **sign** in **each player's own chat as they log in** (server-side only).

Manage your server's announcements, rules, or Discord invite from **signs placed in the world** instead of a config file. Edit the sign, and the new text is shown to everyone who logs in afterwards.

> **Who sees it?** Not just the sign's author — **every player who logs in** (with `kanbanchat.see`) receives it, shown privately in their **own** chat the moment they log in (it is not a server-wide broadcast to everyone currently online).

## The problem it solves

"I want to update my server's announcement in-game, quickly, without editing a config every time."
KanbanChat treats signs as a bulletin board: **write on a sign → it shows on login.**

## Features

- **Two ways to designate a sign (both supported)**
  - Command: look at a sign and run `/kanbanchat set` (all 4 lines usable as body text)
  - Marker: write `[login]` on the sign's first line to auto-register (no command, just place it)
- **Multiple signs concatenated**: each sign has a number; signs are joined into one message in **ascending order**, so you can exceed a single sign's 4-line limit.
- **Shown to each player individually on login**: regardless of who wrote the sign, **every** player who logs in receives it in their own chat (not a server-wide broadcast), a few ticks after join (after the vanilla join message).
- **Formatting preserved**: sign dye colors / decorations carry over into chat.
- **Auto-follows edits**: rewrite the sign and the body updates; break it and it is removed.
- **Global ON/OFF**: pause or resume any time with `/kanbanchat on|off`.

## Requirements

- Server: Paper 26.2 (experimental channel)
- Java: 25
- Clients: vanilla (no mods, server-side only)

## Installation

1. Drop `KanbanChat-1.2.0.jar` into `plugins/` and restart.
2. Place a sign and register it as a "login sign" with either method below.
3. When a player logs in, the contents are shown in their own chat (every player who logs in).

## Usage

### Method 1: register by command

1. Write the body on a sign (up to 4 lines).
2. **Look at** the sign and run `/kanbanchat set`.
   - To assign a number: `/kanbanchat set 2` (auto-numbered if omitted).
   - To use the back side: `/kanbanchat set back`.
3. Use `/kanbanchat test` to preview the message to yourself.

### Method 2: register by marker line

1. Write **`[login]`** (or `[ログイン]`) on the sign's **first line**.
2. Lines 2-4 become the body (the marker line is not included in the message).
3. To set the order, use `[login:2]` (auto-numbered if omitted).

> By default anyone can register signs (both the command and marker methods use `kanbanchat.admin`, default `true`). To prevent griefing, revoke `kanbanchat.admin` from specific players/groups (set it to `false`) with a permissions plugin.

### Concatenating multiple signs

Register several signs and they are joined in **ascending number order** into a single message.
For long announcements, stack signs vertically and number them `1, 2, 3 …`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/kanbanchat set [number] [front\|back]` | Register the sign you are looking at | `kanbanchat.admin` |
| `/kanbanchat remove [number]` | Unregister (by number or by looked-at sign) | `kanbanchat.admin` |
| `/kanbanchat list` | List registered signs | `kanbanchat.admin` |
| `/kanbanchat test` | Preview the message to yourself | `kanbanchat.admin` |
| `/kanbanchat status` | Show the current state | `kanbanchat.admin` |
| `/kanbanchat reload` | Reload config and signs | `kanbanchat.manage` |
| `/kanbanchat on \| off` | Enable / disable | `kanbanchat.manage` |

Alias: `/kc`

## Permissions

| Node | Description | Default |
|---|---|---|
| `kanbanchat.see` | Receive the login message | `true` (everyone) |
| `kanbanchat.admin` | Register/manage signs (set / remove / list / test & marker method) | `true` (everyone) |
| `kanbanchat.manage` | Server-wide operations: `on` / `off` / `reload` | `op` |

> If you previously granted `kanbanchat.admin` to moderators and want them to keep on/off/reload, grant `kanbanchat.manage` as well (ops have both by default).

## Configuration (`config.yml`)

```yaml
enabled: true          # send the message on login (toggled by /kanbanchat on|off)
join-delay-ticks: 30   # wait this many ticks after join before sending (20 ticks = 1s)
markers:               # a first line matching one of these auto-registers the sign
  - "[login]"
  - "[ログイン]"
skip-empty-lines: true # drop blank lines from the message
line-separator: ""     # separator used when joining one sign's lines into a single line (default: none)
```

Registered signs are stored in `plugins/KanbanChat/signs.yml` (body text is cached, so the login message shows instantly even if the sign's chunk is unloaded).

## How it works

- Listens to `SignChangeEvent` to register/unregister marker signs and update command-registered signs.
- The login message is built from the `signs.yml` cache, so it is fast and works even when the sign's chunk is not loaded.
- Changes made **without** an event (e.g. WorldEdit) may desync the cache; run `/kanbanchat reload` to re-sync signs in loaded chunks.
- Sign text is stored as Adventure `Component` (JSON), preserving colors/decoration in chat.

### Limitations

- A Minecraft sign holds 4 lines per side, ~15 characters per line. Use multiple signs for longer text.
- **The multiple lines of a single sign are joined into one chat line (no line breaks); only different signs are separated by line breaks.** Text wrapped by the sign's narrow width reads continuously in chat. The separator is configurable via `line-separator` (default: none).
- The marker method uses line 1 as the marker, leaving 3 body lines (command method keeps all 4).

## Build

```bash
./deploy.sh        # macOS native (JDK 25 + Maven). Output: target/KanbanChat-1.2.0.jar
# or
mvn -B clean package
```

Pushing a `v*` tag triggers GitHub Actions (`.github/workflows/build.yml`) to build and attach the jar to a release.

## Deploying to a server

Put the jar into your server's `plugins/` directory and restart. There are two ways (A/B) to obtain the jar. If you use Docker (itzg/minecraft-server), you can also use the "Docker Compose auto-download" method below.

### A. Use a release build (no build needed, recommended)

Download the latest `KanbanChat-<version>.jar` from [Releases](https://github.com/astail/mc-kanban-chat/releases). No JDK or Maven required.

```bash
# Download the latest release jar (with the gh CLI)
gh release download --repo astail/mc-kanban-chat --pattern '*.jar'
```

### B. Build it yourself

Follow [Build](#build) to produce `target/KanbanChat-1.2.0.jar`.

### Place the jar

Put the jar into the server's `plugins/` and restart.

```bash
# Bind mount (copy into the host plugins directory)
cp target/KanbanChat-1.2.0.jar /path/to/data/plugins/
docker restart <container>

# Named volume etc. (copy directly into the container)
docker cp target/KanbanChat-1.2.0.jar <container>:/data/plugins/
docker restart <container>
```

### Docker Compose (itzg/minecraft-server) auto-download

With the [`itzg/minecraft-server`](https://github.com/itzg/docker-minecraft-server) image, you don't even need the jar on hand — just **list the release URL in the `PLUGINS` environment variable** and it is downloaded into `plugins/` at startup.

```yaml
services:
  mc:
    image: itzg/minecraft-server
    tty: true
    stdin_open: true
    ports:
      - "25565:25565"
    environment:
      EULA: "TRUE"
      TYPE: "PAPER"
      VERSION: "26.2"
      PAPER_CHANNEL: "experimental"
      PLUGINS: |
        https://github.com/astail/mc-kanban-chat/releases/download/v1.2.0/KanbanChat-1.2.0.jar
    volumes:
      - ./data:/data
    restart: unless-stopped
```

`PLUGINS` accepts multiple newline-separated URLs. When you bump the version, update `v1.2.0` and the filename in the URL to match the new release.

You'll know it worked when the startup log shows:

```text
[KanbanChat] KanbanChat を有効化しました（登録看板: 0 枚 / 状態: ON）。
```

## License

MIT License — see [LICENSE](LICENSE).
