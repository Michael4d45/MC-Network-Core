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
- Lang: `assets/network-core/lang/en_us.json` includes block name & command feedback strings; GUI keys may be stubbed until UI implemented.
- (Planned) GUI Screen: Not implemented yet. Current NetworkCoreEntity.createMenu returns null.
- Datapack Tests: `networkcore_test` functions kept in sync with protocol changes (update when frame formats evolve).

## Version Management

- Update versions in `build.gradle` top section when upgrading Minecraft/Fabric
- Check https://fabricmc.net/develop for latest compatible versions
- Java 21 runtime and bytecode target (Loom config sets toolchain + `options.release` to 21)

## Quality Checks

Before committing or releasing:

1. Build succeeds: `./gradlew build`
2. Client starts & block appears in Redstone creative tab
3. Place block, verify:
   - Facing property correct relative to placement look direction
   - Transmit side reads neighbor redstone power (opposite facing)
   - Receive side emits power updates only when frame emission output changes
4. Commands function (see Commands section) – especially `sendtest`, `pauseTickProcess`, `resumeTickProcess`, `listports`, `udpaddress`
5. Protocol invariants: SOF=15, EOF=0, idle=0 upheld; Data (TYPE=0), Control (TYPE=1), IPv4 (TYPE=3) frames parse; Status (TYPE=2) intentionally not produced yet
6. Counters increment appropriately for framing errors, frames parsed/emitted, drops (verify via logs or temporary debug output)
7. No missing resources (logs free of "missing model" / magenta blocks)
8. World save/load cycle:
   - Port & symbol period persist
   - Conflicting ports auto-reassigned without crash/hang
9. Datapack test functions run (e.g., `/function networkcore_test:test_ipv4_max_payload_src_192_168_1_25`) without desync/hang when preceded by `/networkcore pauseTickProcess`
10. Hot-load / reload older saves (if schema changes) does not crash; defaults applied

## World Persistence & Loading Considerations

**CRITICAL**: Always consider world save/load implications when modifying block entities, backend systems, or persistent data structures.

### Block Entity Persistence

- Loading order: `readData()` (storage API) populates fields before backend registration / port claiming.
- Defaults: Omitted `Port` → request new port; omitted `SymbolPeriodTicks` → default 2.
- Validation: Clamp symbol period to [1,8]; clamp port to [-1,65535].
- Version tolerance: Treat absent fields as defaults; never assume presence.

### Backend Registration

- Preserve saved port if available; call `DataRouter.registerExisting()` to reconcile conflicts.
- If registration returns a different port (conflict), update internal value & mark dirty.
- If no valid port (`-1` or missing) request one via `DataRouter.requestPort()` after load.
- Avoid redundant `markDirty()` in load loop unless value actually changes.

### Common Loading Issues

- Do NOT overwrite loaded values before reconciliation.
- Ensure existing cores call `handleLoad()` (invoked after construction) to register with routers.
- Claim unique ports through router; never trust raw NBT without validation.
- Keep operations synchronous & lightweight (server thread only).
- Avoid unnecessary neighbor notifications during chunk population (defer until state actually changes at runtime).

### Testing World Loading

- Place multiple cores, assign custom symbol periods & ports if/when GUI implemented (currently manual/command-driven or internal).
- Save & exit, reload world: verify assignments persist & no duplicate registration logs.
- Simulate invalid/corrupt data by editing NBT (remove `Port` key) – core should request a new port safely.

### Prevention Checklist

- [ ] Does this change affect block entity storage keys (`Port`, `SymbolPeriodTicks`)?
- [ ] Are new persisted fields given safe defaults in `readData()`?
- [ ] Is backend port registration idempotent & non-destructive for existing assignments?
- [ ] Could load-time logic trigger excessive neighbor updates?
- [ ] Have I tested save → quit → load with multiple cores & conflicting ports?
- [ ] Are invalid values clamped before use?

## Protocol & Runtime Notes

- Frame Types implemented: 0 (Data), 1 (Control), 2 (Status), 3 (IPv4).
- Max payload length: 255 nibbles (LEN=0xFF)
- Idle line: continuous 0 nibbles; EOF also 0 (context distinguishes end vs idle)
- SOF strictly 15; parser remains in IDLE until SOF observed
- Control frames: MODEQ currently a no-op (no status frame returned) – future enhancement will emit Type 2.

## Commands (Developer / Testing)

Alias root: `/networkcore` (`/nc` shortcut)

- `sendtest <0-15>`: Inject a symbol into nearest core's TX parser (bypasses timing)
- `pauseTickProcess` / `resumeTickProcess`: Toggle internal tick pacing for deterministic test sequences
- `udpaddress`: Displays current UDP bind/target address used by `IPv4Router`
- `listports`: Dump allocated ports per loaded world with block positions
- `stats`: Show counters + queue depth + error flags for nearest core
- `help`: Command summary

Use pause/resume around datapack symbol sequences to avoid extraneous tick-driven symbols interleaving.

## Testing Aids

- Datapack functions under `networkcore_test` build canonical frames (including max-length IPv4 example)
- Validate IPv4 framing with `/function networkcore_test:test_ipv4_max_payload_src_192_168_1_25`
- Extend by adding new `.mcfunction` files that leverage `/networkcore sendtest` to express nibble streams.

## Counters & Telemetry (Current Scope)

- `txFramesParsed`
- `txFramesDropped`
- `rxFramesEmitted`
- `txFramingErrors`

Planned: `rxOverflowDrops`, `unroutableFrames`, `loopbackFrames`, error flag bitfield reporting via Status frames.

## Future Enhancements (Do Not Prematurely Implement Without Spec Alignment)

- Status frame emission & MODEQ response
- GUI screen (port & symbol period) with validation and server sync
- Expanded error counters & user-facing diagnostics
- Checksum or integrity nibble(s)
- Cross-chunk routing and multi-hop forwarding

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

This guide reflects the current (no GUI, no status frame) state. Update sections above when implementing planned features to keep tooling & contributors aligned.
