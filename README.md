# Network Core

Deterministic, NIC-inspired redstone ↔ packet interface for Fabric Minecraft 1.21.7.

## Overview

Each `Network Core` block acts like a very small network interface controller (NIC). It samples an incoming redstone power level every symbol period (default 2 ticks) and interprets nibbles (0–15) as a serial protocol. (This could have been on or off, but we're taking advantage of redstone power levels). Frames are emitted back to the world as redstone power on the block's facing side. The block persists its assigned port and symbol period across world saves.

Implemented frame types:

- TYPE=0 Data Frames
- TYPE=1 Control Frames (NOP, RESET, MODEQ (status request), SETPER, SETPORT, STATSCLR) – status response not yet produced
- TYPE=3 IPv4 Frames (bridged to/from an external UDP endpoint via `IPv4Router`)

Planned / Not yet implemented:

- TYPE=2 Status Frame emission in response to MODEQ
- GUI configuration screen (currently right-click does nothing / opens no UI)
- Additional telemetry counters (RX overflow, unroutable, loopback stats)

## Current Features

- Fixed nibble wire protocol (SOF=15, EOF=0, idle=0)
- Data, Control, IPv4 frame parsing & emission
- Port allocation & persistence (0–65535 per world; automatic assignment if unset)
- Symbol period (1–8 ticks) persisted; clamped & validated on load
- Basic counters: framing errors, frames parsed/emitted, drops
- IPv4 frame mapping (TYPE=3) including IP, UDP port, and in‑game port addressing
- Commands for low-level testing & inspection
- Test datapack with scripted frame injections (see `networkcore_test/`)

## Crafting

You can craft the Network Core block with a shaped recipe:

Pattern (Crafting Table 3×3):

|  |  |  |
| --- | --- | --- |
| <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> |
| <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone_Comparator.png" alt="Comparator" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> |
| <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> |

## Protocol Quick Reference

See the full spec in [`NETWORK_CORE_PROTOCOL.md`](NETWORK_CORE_PROTOCOL.md). Highlights:

- Max payload: 255 nibbles
- Data header: 15 nibbles total (TYPE + 14) + LEN + payload + EOF
- IPv4 header: 39 nibbles (TYPE + 38) + LEN + payload + EOF Implemented frame types:
- TYPE=0 Data Frames
- TYPE=1 Control Frames (NOP, RESET, MODEQ (status request), SETPER, SETPORT, STATSCLR)
- TYPE=2 Status Frames (8‑nibble payload) emitted on MODEQ
- TYPE=3 IPv4 Frames (bridged to/from external UDP endpoint)
- Idle channel is continuous 0 nibbles; framing recovery waits for SOF=15

## Persistence & World Safety

- TYPE=2 Status Frames (8‑nibble payload) emitted on MODEQ Block entity NBT stores only:

- Port (key `Port`) – may be -1 internally during load reconciliation
- Symbol period (key `SymbolPeriodTicks`) – omitted when default (2)

On load the block re-registers its saved port with the `DataRouter`; conflicts trigger reassignment (clamped and persisted). Loading remains non-destructive: invalid or absent values fall back to defaults.

## Commands

All commands start with `/networkcore` (alias `/nc`). Operator permission required.

- `sendtest <0-15>`: Inject a single test symbol into the nearest core's TX parser immediately
- `pauseTickProcess`: Pause symbol period ticking for nearest core (for deterministic test injection)
- Counters: framing errors, frames parsed/emitted, RX overflow drops (exposed via `/networkcore stats` and status frame error flags)
- `udpaddress`: Show the current UDP address/port used for external IPv4 bridging
- `listports`: List all allocated ports per loaded world (position → port)
- `help`: Display summary

Example (datapack) sequence: see `networkcore_test/data/networkcore_test/function/*.mcfunction` such as `test_ipv4_max_payload_src_192_168_1_25.mcfunction` for constructing a full IPv4 frame (TYPE=3) with max payload length 255.

## Testing With Datapack

Copy or enable the included datapack folder `networkcore_test` in your test world. Use:

```
/function networkcore_test:test_ipv4_max_payload_src_192_168_1_25
```

Pause ticking before scripted symbol series when you need to feed exact timing:

```
/networkcore pauseTickProcess
... (symbol injection) ...
 - `stats`: Show live counters for nearest core
/networkcore resumeTickProcess
```

## Building

Windows PowerShell / \*nix shells:

```
./gradlew build
```

Artifacts appear under `build/libs/` (remapped jar plus sources jar if enabled).

## Running Client

```
./gradlew runClient
```

## Formatting

The project uses Spotless:

```
./gradlew format       # Apply
./gradlew formatCheck  # Verify
```

## Project Structure (selected)

- `src/main/java` – Mod source (`NetworkCore`, block/entity, routers)
- `src/main/resources` – Assets & data packs (blockstates, models, language, loot tables)
- `networkcore_test` – Datapack with function scripts for integration tests
- `NETWORK_CORE_PROTOCOL.md` – Full protocol specification

## Version Matrix

Target (as configured):

- Minecraft: 1.21.7
- Loader: 0.17.2
- Fabric API: 0.129.0+1.21.7
- Java: 21 (toolchain + release)

## Contributing

1. Fork & branch
2. Make change(s) + add tests/datapack scripts as needed
3. Run `./gradlew build` (and `test` when tests are added)
4. Submit PR with rationale & reproduction steps

## License

MIT – see [`LICENSE`](LICENSE)

## Credits

Design & implementation: Michael
