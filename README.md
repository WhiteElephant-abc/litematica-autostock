# Litematica AutoStock

Client-side Litematica addon that collects missing building materials from nearby containers using Baritone.

## How it works

1. Reads the selected Litematica placement's missing material list.
2. Scans container block entities within a configurable radius around the player, nearest first.
3. Reads their contents through Servux and keeps the containers that hold needed items.
4. Paths to them with Baritone, opens them silently, and moves exactly the missing amounts into the player inventory.
5. Stops as soon as every missing item is satisfied.

The container GUI is never shown; clicks are sent directly to the server.

## Requirements

- Minecraft 26.3 with Fabric Loader 0.20.1+
- Fabric API and Fabric Language Kotlin
- Litematica (sakura-ryoko) and MaLiLib
- Baritone (Fabric, 26.3 branch). A Baritone API jar is included under `libs/`.
- Servux on the server; container contents are read through the `servux:litematics` channel
- Optional: MiniHUD for the search range cylinder

## Usage

```
#stock
#stock <item> [count] [<item> [count] ...]
```

- `#stock` collects the missing materials of the currently selected Litematica placement.
- `#stock minecraft:powered_rail 30 minecraft:lever 5` collects the given items and amounts instead, ignoring the schematic list.
- An item without a count uses the configured default item count (1 by default).
- The scan center is the player's position when the command is run.

## Configuration

The config screen is registered with MaLiLib (mod list -> Litematica AutoStock). Options:

- `searchRadius` — horizontal search radius (default 16)
- `verticalRange` — vertical search range (default 16)
- `containerWhitelist` — block IDs to scan; empty means all containers (default: chests, trapped chests, barrels and all shulker boxes)
- `defaultItemCount` — count used when an item is given without an explicit count
- `allowBreakDuringPathing` — whether Baritone may break blocks while pathing (default off)
- `allowPlaceDuringPathing` — whether Baritone may place blocks while pathing (default on)
- `showSearchRange` — draw the search range with MiniHUD

Configs are saved to `config/litematica-autostock.json`.

## Build

```
./gradlew build
```

The Baritone API jar is loaded from `libs/baritone*.jar`.

## License

GPL-3.0-or-later. The bundled Baritone API jar is third-party software available under the LGPL-3.0.
