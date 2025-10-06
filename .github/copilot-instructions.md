# Fabric Minecraft Mod Development Guide (Network Core)

Never update gradle.properties. According to https://fabricmc.net/develop, the latest versions for 1.21.7 are:

minecraft_version=1.21.7 yarn_mappings=1.21.7+build.8 loader_version=0.17.2 loom_version=1.11-SNAPSHOT

# Fabric API

fabric_version=0.129.0+1.21.7

## Resources Checklist

Keep assets internally consistent; missing model/texture pairs will render as magenta/black.

- Blockstates: `assets/network-core/blockstates/network_core.json` – must account for `facing`, `receive_active`, `transmit_active` (power level models may be coarse-grained if variants consolidated). Ensure variants exist for each referenced model.
- Models: base + per-state overlays as needed (e.g., receive/transmit powered). Minimize combinatorial explosion (optional: unify powered levels via tint or single powered model).
- Item Model: `assets/network-core/models/item/network_core.json` referencing the block model.
- Textures: All referenced textures present under `assets/network-core/textures/block/`.
- Loot Table: `data/network-core/loot_tables/blocks/network_core.json` – drops itself; includes explosion survival predicate.
- Lang: `assets/network-core/lang/en_us.json` includes block name & command feedback strings.
- No GUI: Configuration occurs exclusively through control frames (SETPORT) and commands.
- Datapack Tests: `networkcore_test` functions kept in sync with protocol changes (update when frame formats evolve).

## Version Management

**Current Versions (as of gradle.properties):**

- Minecraft: 1.21.7
- Yarn Mappings: 1.21.7+build.8
- Fabric Loader: 0.17.2
- Loom: 1.11-SNAPSHOT
- Fabric API: 0.129.0+1.21.7
- Java: 21 (runtime and bytecode target)

**Updating:**

- Check https://fabricmc.net/develop for latest compatible versions
- Update `gradle.properties` (NOT `build.gradle`)
- Run `./gradlew --refresh-dependencies build`
- Test `runClient` for mapping compatibility

## Quality Checks

Before committing or releasing:

1. Build succeeds: `./gradlew build`
2. Client starts & block appears in Redstone creative tab
3. Place block, verify:
   - Facing property correct relative to placement look direction
   - Transmit side reads neighbor redstone power (opposite facing)
   - Receive side emits power updates only when frame emission output changes
   - Clock gating works: block processes symbols only when clock faces powered
4. Commands function (see Commands section) – especially `sendtest`, `listports`, `udpaddress`, `stats`
5. Protocol invariants: SOF=15, EOF=0, idle=0 upheld; Data(0), Control(1), Status(2), IPv4(3), IPv4Control(4) frames parse
6. Counters increment appropriately for framing errors, frames parsed/emitted, drops (verify via logs or temporary debug output)
7. No missing resources (logs free of "missing model" / magenta blocks)
8. World save/load cycle:
   - Port persist
   - Conflicting ports auto-reassigned without crash/hang
9. Datapack test functions run (e.g., `/function networkcore_test:test_ipv4_max_payload_src_192_168_1_25`)
10. Hot-load / reload older saves (if schema changes) does not crash; defaults applied

## World Persistence & Loading Considerations

**CRITICAL**: Always consider world save/load implications when modifying block entities, backend systems, or persistent data structures.

**Port Allocation Architecture**: Ports are **world-agnostic** (apply to entire Minecraft instance, not per-world). BlockPos coordinates are unique per world; `getBlockEntity(pos)` is world-scoped, preventing collisions between identical positions in Overworld/Nether/End. The DataRouter iterates all loaded worlds when resolving ports.

### Block Entity Persistence

- Loading order: `readData()` (storage API) populates fields before backend registration / port claiming.
- Defaults: Omitted `Port` → request new port.
- Validation: Clamp port to [-1,65535].
- Version tolerance: Treat absent fields as defaults; never assume presence.

### Backend Registration

- Preserve saved port if available; call `DataRouter.registerExisting()` to reconcile conflicts.
- If registration returns a different port (conflict), update internal value & mark dirty.
- If no valid port (`-1` or missing) request one via `DataRouter.requestPort()` after load.
- Avoid redundant `markDirty()` in load loop unless value actually changes.

### Common Loading Issues

- Do NOT overwrite loaded values before reconciliation.
- Claim unique ports through router; never trust raw NBT without validation.
- Keep operations synchronous & lightweight (server thread only).
- Avoid unnecessary neighbor notifications during chunk population (defer until state actually changes at runtime).

### Prevention Checklist

- [ ] Does this change affect block entity storage keys (`Port`)?
- [ ] Are new persisted fields given safe defaults in `readData()`?
- [ ] Is backend port registration idempotent & non-destructive for existing assignments?
- [ ] Could load-time logic trigger excessive neighbor updates?
- [ ] Have I tested save → quit → load with multiple cores & conflicting ports?
- [ ] Are invalid values clamped before use?

## Protocol & Runtime Notes

- Frame Types implemented: 0 (Data), 1 (Control), 2 (Status), 3 (IPv4), 4 (IPv4 Control).
- Data Control codes: NOP, PORT_UNREACHABLE, MALFORMED_FRAME (accepted but not emitted), BLOCK_BUSY, ECHO_REQUEST, ECHO_REPLY, MODEQ, RESET, SETPORT
- IPv4 Control codes: NETWORK_UNREACHABLE, HOST_UNREACHABLE, PORT_UNREACHABLE, ECHO_REQUEST, ECHO_REPLY, PARAMETER_PROBLEM, MODEQ, TARGET_BUSY
- Max payload length: 255 nibbles (LEN=0xFF) for all frame types
- Data frame LEN field: Counts **total args** (8 port nibbles + payload), consistent with all other frame types. Max payload = 247 nibbles.
- Data frame CODE field: Must be 0x0 for standard frames; non-zero values reserved and will log warnings.
- IPv4 frame structure: 24 nibbles addressing + 4 nibbles inner header + inner payload
- Idle line: continuous 0 nibbles; EOF also 0 (context distinguishes end vs idle)
- SOF strictly 15; parser remains in IDLE until SOF observed
- Status frames (TYPE=2) emitted on MODEQ control frame; payload matches spec (signature + port + RX depth + error flags).
- Error flags: bit0=RX_OVERFLOW, bit1=TX_FRAMING_ERR, bit2=PORT_ALLOC_FAILURE (reserved), bit3=IPV4_ROUTING_FAILURE (reserved)
- Frame ordering: FIFO processing, no priority between frame types, IPv4-delivered frames queued via `DataRouter.server.execute()`

## Commands (Developer / Testing)

Root: `/networkcore` or `/nc` (shorthand alias)

- `sendtest <0-15>`: Inject a symbol into nearest core's TX parser (bypasses timing)
- `udpaddress`: Display current UDP bind address used by `IPv4Router`
- `listports`: List all allocated ports with block positions and worlds
- `stats`: Show counters, queue depth, and error flags for nearest core
- `help`: Command summary

All commands require operator permissions.

## Testing Aids

- Datapack functions under `networkcore_test` build canonical frames (including max-length IPv4 example)
- Validate IPv4 framing with `/function networkcore_test:test_ipv4_max_payload_src_192_168_1_25`
- Extend by adding new `.mcfunction` files that leverage `/networkcore sendtest` to express nibble streams.

When implementing these, update README + protocol spec first to lock framing semantics.

## Version Bumping Procedure

1. Check latest versions at https://fabricmc.net/develop
2. Update `build.gradle` (NOT `gradle.properties`) for Minecraft, mappings, loader, loom, Fabric API
3. Run `./gradlew --refresh-dependencies build`
4. Launch `runClient` to ensure no mapping breaks (fix any renamed methods/fields)
5. Update README version matrix

## Code Review Quicklist

- Storage: New fields have defaults & clamps
- Networking: Frame parser changes preserve existing framing (SOF=15, EOF=0) or version-gate them
- Redstone: Avoid infinite update loops (only notify neighbors on state change)
- Performance: No heavy allocation in per-tick path (`tick()` minimal)
- Logging: Debug logs not excessively spammy in hot paths
- Tests/Datapack: Updated scripts for any framing changes

---
