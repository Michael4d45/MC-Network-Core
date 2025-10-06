# Network Core

Deterministic, NIC-inspired redstone ↔ packet interface for Fabric Minecraft 1.21.7.

## Overview

Each `Network Core` block acts like a minimal network interface. When placed, it assigns itself a unique port (0–65535) and persists it across world saves.

**Operation:**

- **Transmit (T):** The face opposite the block's orientation samples incoming redstone power as nibbles (0–15)
- **Receive (R):** The oriented face emits outgoing frames as redstone power levels
- **Clock (C):** Any of the four remaining orthogonal faces; when powered, enables symbol processing (allows deterministic external clocking)
- Ticks every 2 game ticks when clock is active; idle line = 0

## Implemented Frame Types

- TYPE=0 Data (with application-defined CODE field)
- TYPE=1 Data Control (NOP, PORT_UNREACHABLE, MALFORMED_FRAME, BLOCK_BUSY, ECHO_REQUEST, ECHO_REPLY, MODEQ, RESET, SETPORT, STATUS_REPLY, HOST_UNREACHABLE, NETWORK_ERROR, TARGET_BUSY)
- TYPE=3 IPv4 (encapsulates Data/Data Control frames, bridged via UDP)

## Current Features

- Fixed nibble wire protocol (SOF=15, EOF=0, idle=0)
- Data, Control, Status, IPv4 frame parsing & emission
- Port allocation & persistence (0–65535 per world)
- Counters: txFramesParsed, rxFramesEmitted, txFramingErrors, rxOverflowDrops (via `/networkcore stats` & STATUS_REPLY frame)
- IPv4 frame mapping (TYPE=3) including IP + UDP + in‑game addressing
- Commands for low-level testing & inspection
- Datapack with scripted frames (`networkcore_test/`)

## Crafting

You can craft the Network Core block with a shaped recipe:

Pattern (Crafting Table 3×3):

| <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> |
| <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone_Comparator.png" alt="Comparator" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> |
| <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> |

Produces: 1× Network Core

## Protocol

All communication uses nibble-based framing (4-bit values 0–15) transmitted via redstone power levels. See [`NETWORK_CORE_PROTOCOL.md`](NETWORK_CORE_PROTOCOL.md) for complete frame specifications.

**Quick Reference:**

- Frame structure: SOF(15) + TYPE + CODE + LEN_HI + LEN_LO + ARGS + EOF(0)
- Max payload: 247 nibbles for Data frames, 255 for others
- Idle/resync: continuous 0 nibbles until SOF=15
- All frame types and control codes documented in protocol spec

## Persistence

Stored NBT keys:

- `Port` (may be reassigned on conflict)

On load the saved port is reconciled via `DataRouter`; invalid / missing values get a fresh port.

## Commands

Root `/networkcore` (or `/nc` shorthand), operator required:

- `sendtest <0-15>` — inject a nibble into nearest core's TX parser
- `udpaddress` — show current UDP bind address for IPv4 routing
- `listports` — list all allocated ports with block positions and worlds
- `stats` — show counters, queue depth, and error flags for nearest core
- `help` — command summary

## Datapack Testing

Use functions under `networkcore_test` to emit canonical frames, e.g.:

```
/function networkcore_test:test_ipv4_max_payload_src_192_168_1_25
```

Artifacts land in `build/libs/`.

## Running (Client Dev)

```
./gradlew runClient
```

## Formatting

```
./gradlew format
./gradlew formatCheck
```

## Structure

- `src/main/java` core sources
- `src/main/resources` assets + data
- `networkcore_test` datapack frames
- `NETWORK_CORE_PROTOCOL.md` protocol spec

## Versions

- Minecraft 1.21.7
- Yarn Mappings 1.21.7+build.8
- Fabric Loader 0.17.2
- Loom 1.11-SNAPSHOT
- Fabric API 0.129.0+1.21.7
- Java 21

## Contributing

1. Branch
2. Implement / adjust datapack scripts if wire format changes
3. `./gradlew build`
4. Open PR with rationale

## License

MIT – see [`LICENSE`](LICENSE)

## Credits

Design & implementation: Michael
