# Fabric Minecraft Mod Development Guide

Never update gradle.properties. According to https://fabricmc.net/develop, the latest versions for 1.21.7 are:

minecraft_version=1.21.7 yarn_mappings=1.21.7+build.8 loader_version=0.17.2 loom_version=1.11-SNAPSHOT

# Fabric API

fabric_version=0.129.0+1.21.7

## Resources Checklist

- **Blockstates**: one entry per facing + receive/transmit combo (see `blockstates/network_core.json`)
- **Models**: base + powered variants (e.g., `network_core_receive_powered.json`)
- **Item Model**: references the block model
- **Textures**: ensure each model referenced texture exists in `textures/block/`
- **Loot Table**: `data/network-core/loot_tables/blocks/network_core.json` drops the block item with explosion survival guard
- **Lang**: include block name plus any screen strings under `screen.network-core.*`
- **GUI Screen**: Implement screen handler and screen classes for block configuration

## Version Management

- Update versions in `build.gradle` top section when upgrading Minecraft/Fabric
- Check https://fabricmc.net/develop for latest compatible versions
- Java 21 runtime and bytecode target (Loom config sets toolchain + `options.release` to 21)

## Quality Checks

- Build succeeds: `./gradlew build`
- Client runs: `./gradlew runClient`
- No missing resources (check logs for magenta/black cubes)
- Block appears in creative tab, GUI opens, redstone toggles based on backend updates
- State machines parse/emit frames correctly (run tests: `./gradlew test`)
- Protocol wire format matches `NETWORK_CORE_PROTOCOL.md` specification
- World loads without hanging (test with existing saves containing blocks)
- Test datapack functions work: place block, run `/function networkcore_test:test_frame_simple`

## World Persistence & Loading Considerations

**CRITICAL**: Always consider world save/load implications when modifying block entities, backend systems, or persistent data structures.

### Block Entity Persistence

- **NBT Loading Order**: Block entities load from disk BEFORE backend registration occurs
- **Default Values**: Ensure `readNbt()` provides sensible defaults for missing data
- **Version Compatibility**: Handle missing fields gracefully when loading older saves
- **Validation**: Clamp/validate loaded values to prevent corrupted worlds

### Backend Registration

- **Preserve Existing State**: When registering blocks during chunk load, check for existing valid state before assigning new values
- **Conflict Resolution**: Handle cases where saved data conflicts with current logic (e.g., duplicate ports)
- **Graceful Fallbacks**: If saved state is invalid, assign new valid state rather than failing

### Common Loading Issues

- **Overwriting Saved Data**: Backend registration should NOT blindly overwrite block entity state loaded from disk
- **Missing Registration**: Ensure blocks register with backend during chunk load, not just placement
- **Port/ID Conflicts**: Unique identifiers (ports, IDs) must be claimed from managers, not assumed available
- **Threading**: World loading happens on server thread - avoid blocking operations
- **Chunk Load Modifications**: Avoid calling `markDirty()` or `updateListeners()` on block entities during chunk load events to prevent deadlocks or hangs. Defer world notifications until user-initiated changes

### Testing World Loading

- **Save & Reload**: Test placing blocks, saving world, then reloading to verify persistence
- **Version Upgrades**: Test loading worlds saved with previous mod versions
- **Error Recovery**: Ensure corrupted data doesn't prevent world loading (provide defaults)

### Prevention Checklist

- [ ] Does this change affect block entity NBT serialization?
- [ ] Does this change backend registration logic?
- [ ] Could this overwrite existing saved state?
- [ ] Does this change modify block entities during chunk load events?
- [ ] Have I tested save/load cycles?
- [ ] Are there graceful fallbacks for invalid saved data? <parameter name="filePath">c:\Users\Michael\Projects\Java\IP mod\.github\copilot-instructions.md
