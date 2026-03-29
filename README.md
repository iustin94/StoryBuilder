# QuestForge

A quest management system for Hytale servers, consisting of a Java server plugin and a browser-based visual quest editor.

## Components

### Plugin (`plugin/`)

A Hytale server plugin that provides:

- **Quest state machine engine** — quests are directed graphs of states (dialogue, objectives, branches) with conditional transitions
- **NPC interaction** — quests are assigned to NPCs by role name (not UUID), making assignments portable across servers
- **Objective integration** — delegates task tracking (kill, gather, location) to Hytale's built-in `ObjectivePlugin`
- **REST API** — embedded HTTP server on port 7432 for the web editor to read/write quests and NPC assignments
- **Hot reload** — quest changes take effect without restarting the server

### Web Editor (`site/`)

A Hugo static site that provides:

- **Manager screen** — drag-and-drop NPC-to-quest assignment
- **Graph Editor** — visual SVG node editor for quest state machines with pan/zoom
- **Settings** — server connection configuration and offline sync management
- **Offline support** — operations queue in localStorage, auto-sync when server is reachable

## Building

### Plugin

Requires Java 21.

```bash
./build.sh
# or
./gradlew :plugin:build
```

The built JAR is at `plugin/build/libs/questforge-1.0.0.jar`.

### Web Editor

Requires [Hugo](https://gohugo.io/).

```bash
cd site
hugo
```

The built site is in `site/public/`. Serve it with any static file server, or run `hugo server` for local development.

## Quest JSON Format

Quests are JSON files stored in `plugins/questforge/quests/`. Each quest is a state machine:

```json
{
  "questId": "lost_blade",
  "questName": "The Lost Blade",
  "startState": "n1",
  "variables": {
    "reputation": { "type": "integer", "initial": 0 }
  },
  "states": {
    "n1": {
      "type": "start",
      "title": "Quest Begin",
      "transitions": [
        { "label": "I can help", "to": "n2" },
        { "label": "Not my problem", "to": "n3" }
      ]
    }
  }
}
```

### State Types

| Type | Purpose |
|------|---------|
| `start` | Entry point — fans out into first choices |
| `dialogue` | NPC speech with response buttons |
| `objective` | Delegates to ObjectivePlugin for task tracking |
| `branch` | Silent routing based on variable conditions |
| `end` | Terminal state — marks quest complete/failed |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/questforge/status` | Health check |
| GET | `/questforge/npcs` | List NPC roles |
| GET | `/questforge/quests` | List all quests |
| POST | `/questforge/quest` | Save a quest |
| DELETE | `/questforge/quest/{id}` | Delete a quest |
| POST | `/questforge/assign` | Assign NPC to quest |
| POST | `/questforge/unassign` | Unassign NPC from quest |
| POST | `/questforge/reload` | Hot reload quests |

## License

[Unlicense](LICENSE) — public domain.
