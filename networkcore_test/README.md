# NetworkCore Test Datapack

This datapack supplies helper test functions for the NetworkCore block / protocol.

## Functions (run with /function namespace:path)
- networkcore_test:load  (auto via minecraft:load)
- networkcore_test:test_simple  (chat sanity message)
- networkcore_test:test_frame_simple  (LEN=6 frame, dst=0x34 src=0x12, no payload)
- networkcore_test:test_frame_with_payload  (LEN=8 frame, payload 0xA 0xB)
- networkcore_test:test_control_setport  (Control SETPORT -> port 0x2A)
- networkcore_test:test_invalid_frame  (Malformed EOF to exercise error path)
- networkcore_test:test_reset  (Control RESET placeholder)

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
