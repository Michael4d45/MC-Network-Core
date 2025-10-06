# NetworkCore Test Datapack

This datapack supplies helper test functions for the NetworkCore block / protocol.

## Installation

To install this datapack in your test world, run:

```bash
./gradlew copyDatapack
```

This will copy the datapack to `run/saves/New World/datapacks/networkcore_test`.

## Functions (run with /function namespace:path)

- networkcore_test:load (auto via minecraft:load)
- networkcore_test:test_simple (chat sanity message)
- networkcore_test:test_frame_simple (TYPE=0, LEN=0x08, dst=0x002A src=0x0012)
- networkcore_test:test_frame_with_payload (TYPE=0, LEN=0x0A, payload 0x0A 0x0B)
- networkcore_test:test_frame_max_payload (TYPE=0, LEN=0xF7, 247-nibble payload helper)
- networkcore_test:test_control_setport (Control SETPORT -> port 0x0042)
- networkcore_test:test_invalid_frame (Malformed EOF to exercise error path)
- networkcore_test:test_reset (Control RESET placeholder)
- networkcore_test:test_ipv4_to_simple (IPv4 TYPE=3 LEN=0x24, encapsulated DataFrame w/out payload)
- networkcore_test:test_ipv4_from_simple (IPv4 TYPE=3 LEN=0x24, inbound DataFrame w/out payload)
- networkcore_test:test_ipv4_to_with_payload (IPv4 TYPE=3 LEN=0x26, DataFrame payload [0x0A,0x0B])
- networkcore_test:test_ipv4_from_with_payload (IPv4 TYPE=3 LEN=0x26, inbound DataFrame payload [0x0A,0x0B])
- networkcore_test:test_ipv4_max_payload (IPv4 raw payload LEN=0xE3 helper cycles)
- networkcore_test:test_ipv4_max_payload_src_192_168_1_25 (IPv4 encapsulated DataFrame LEN=0xE3)
- networkcore_test:test_ipv4_data_control_echo_reply (IPv4-encapsulated Data Control ECHO_REPLY)
- networkcore_test:test_ipv4_data_control_modeq (IPv4-encapsulated Data Control MODEQ)

## Removed

The old plural directory `functions/` was replaced by the active `function/` folder.

## Disabling Heartbeat

If present, remove `data/minecraft/tags/function/tick.json` or delete `tick.mcfunction` to stop chat spam.

## Usage Flow

1. Place a NetworkCore block and note its coordinates.
2. Run one of the frame functions.
3. Inspect backend: `/networkcore status <x> <y> <z>`
4. Observe server log for frame commit or error transitions.

## Maintenance

- Keep commands minimal; each line is one symbol injection command.
- Extend with additional frames for edge cases (max length, overflow, etc.).
