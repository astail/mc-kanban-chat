# KanbanChat

A Paper plugin that shows the contents of a designated **sign** in a player's **chat when they log in** (server-side only).

Manage your server's announcements, rules, or Discord invite from **signs placed in the world** instead of a config file. Edit the sign, and the new text is shown to everyone who logs in afterwards.

## The problem it solves

"I want to update my server's announcement in-game, quickly, without editing a config every time."
KanbanChat treats signs as a bulletin board: **write on a sign → it shows on login.**

## Features

- **Two ways to designate a sign (both supported)**
  - Command: look at a sign and run `/kanbanchat set` (all 4 lines usable as body text)
  - Marker: write `[login]` on the sign's first line to auto-register (no command, just place it)
- **Multiple signs concatenated**: each sign has a number; signs are joined into one message in **ascending order**, so you can exceed a single sign's 4-line limit.
- **Sent to the joining player only**, a few ticks after join (after the vanilla join message).
- **Formatting preserved**: sign dye colors / decorations carry over into chat.
- **Auto-follows edits**: rewrite the sign and the body updates; break it and it is removed.
- **Global ON/OFF**: pause or resume any time with `/kanbanchat on|off`.

## Requirements

- Server: Paper 26.1.2 (build 69+)
- Java: 25
- Clients: vanilla (no mods, server-side only)

## Installation

1. Drop `KanbanChat-1.0.0.jar` into `plugins/` and restart.
2. Place a sign and register it as a "login sign" with either method below.
3. When a player logs in, the contents are shown in their chat.

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

> Only players with `kanbanchat.admin` can register via the marker method (anti-grief).

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
| `/kanbanchat reload` | Reload config and signs | `kanbanchat.admin` |
| `/kanbanchat on \| off \| status` | Enable / disable / show state | `kanbanchat.admin` |

Alias: `/kc`

## Permissions

| Node | Description | Default |
|---|---|---|
| `kanbanchat.see` | Receive the login message | `true` (everyone) |
| `kanbanchat.admin` | Use the management commands | `op` |

## Configuration (`config.yml`)

```yaml
enabled: true          # send the message on login (toggled by /kanbanchat on|off)
join-delay-ticks: 30   # wait this many ticks after join before sending (20 ticks = 1s)
markers:               # a first line matching one of these auto-registers the sign
  - "[login]"
  - "[ログイン]"
skip-empty-lines: true # drop blank lines from the message
```

Registered signs are stored in `plugins/KanbanChat/signs.yml` (body text is cached, so the login message shows instantly even if the sign's chunk is unloaded).

## How it works

- Listens to `SignChangeEvent` to register/unregister marker signs and update command-registered signs.
- The login message is built from the `signs.yml` cache, so it is fast and works even when the sign's chunk is not loaded.
- Changes made **without** an event (e.g. WorldEdit) may desync the cache; run `/kanbanchat reload` to re-sync signs in loaded chunks.
- Sign text is stored as Adventure `Component` (JSON), preserving colors/decoration in chat.

### Limitations

- A Minecraft sign holds 4 lines per side, ~15 characters per line. Use multiple signs for longer text.
- The marker method uses line 1 as the marker, leaving 3 body lines (command method keeps all 4).

## Build

```bash
./deploy.sh        # macOS native (JDK 25 + Maven). Output: target/KanbanChat-1.0.0.jar
# or
mvn -B clean package
```

Pushing a `v*` tag triggers GitHub Actions (`.github/workflows/build.yml`) to build and attach the jar to a release.

## License

MIT License — see [LICENSE](LICENSE).
