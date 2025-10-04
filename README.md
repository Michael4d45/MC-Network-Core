# Network Core

Deterministic, NIC-inspired redstone ↔ packet interface for Fabric Minecraft 1.21.7.

## Overview

Each `Network Core` block acts like a minimal network interface. It samples a redstone power level every game tick if clock is powered on the `T` face (Transmitting) interpreting it as a nibble (0–15). Frames are emitted as redstone power on the `R` face (Receiving). The block persists its assigned port across world saves. A optional clock gating input enables externally clocked deterministic capture.

## Implemented Frame Types

- TYPE=0 Data
- TYPE=1 Control (NOP, RESET, MODEQ, SETPORT, STATSCLR)
- TYPE=2 Status (8‑nibble payload emitted on MODEQ)
- TYPE=3 IPv4 (bridged via UDP)

## Current Features

- Fixed nibble wire protocol (SOF=15, EOF=0, idle=0)
- Data, Control, Status, IPv4 frame parsing & emission
- Port allocation & persistence (0–65535 per world)
- Counters: txFramesParsed, txFramesDropped, rxFramesEmitted, txFramingErrors, rxOverflowDrops (via `/networkcore stats` & status frame flags)
- IPv4 frame mapping (TYPE=3) including IP + UDP + in‑game addressing
- Commands for low-level testing & inspection
- Datapack with scripted frames (`networkcore_test/`)

## Crafting

You can craft the Network Core block with a shaped recipe:

Pattern (Crafting Table 3×3):

|  |  |  |
| --- | --- | --- |
| <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> |
| <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone_Comparator.png" alt="Comparator" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> |
| <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Redstone.png" alt="Redstone Dust" width="32" /> | <img src="https://minecraft.wiki/images/Invicon_Iron_Ingot.png" alt="Iron Ingot" width="32" /> |

Produces: 1× Network Core

## Protocol Quick Reference

See [`NETWORK_CORE_PROTOCOL.md`](NETWORK_CORE_PROTOCOL.md) for full detail.

Highlights:

- Max payload: 255 nibbles
- Data header: 15 nibbles (TYPE + 14) + LEN + payload + EOF
- IPv4 header: 39 nibbles (TYPE + 38) + LEN + payload + EOF
- Status frame (TYPE=2) payload:
  - [0]=0xA signature
  - [1-2]=world (hi, lo)
  - [3-4]=port (hi, lo)
  - [5]=RX queue depth (0–13 cap)
  - [6]=TX queue depth (currently always 0)
  - [7]=error flags (bit0 RX_OVERFLOW, bit1 TX_FRAMING_ERR)
- Idle line = 0s; resync waits for SOF=15

## Persistence

Stored NBT keys:

- `Port` (may be reassigned on conflict)

On load the saved port is reconciled via `DataRouter`; invalid / missing values get a fresh port.

## Commands

Root `/networkcore` (alias `/nc`), operator required:

- `sendtest <0-15>` inject a nibble into nearest core
- `udpaddress` show current UDP endpoint
- `listports` list allocated ports with positions
- `stats` show counters, queue depth, error flags
- `help` summary

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
