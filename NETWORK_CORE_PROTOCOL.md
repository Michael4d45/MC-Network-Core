# NetworkCore & Protocol Design

Status: v1 (Framing locked) Owner: NetworkCore subsystem Scope: Minecraft Fabric 1.21.1 – `NetworkCoreBlock` + `NetworkCoreBlockEntity` + `NetworkCoreBackend`

---

## 1. Objectives

Provide a deterministic, NIC-inspired redstone-to-packet mediation layer with:

- Host (player contraption) → NIC TX (TX ring)
- NIC → Host RX (RX ring)
- Bounded FIFO rings (drop on overflow)
- Fixed nibble-based wire protocol

**Non-Goals (Phase 1):** cryptography, reliability, chunk-border bridging, checksum.

---

## 2. Conceptual Layering

| Layer | Real NIC Equivalent | NetworkCore Responsibility                                     |
| ----- | ------------------- | -------------------------------------------------------------- |
| L1    | Physical            | Redstone power levels (0–15) sampled/emitted per symbol period |
| L2    | Data Link (MAC)     | Frame delimiting, length                                       |
| L3+   | Network / Transport | Port-based virtual channels, routing                           |

---

## 3. Wire / Symbol Protocol

### 3.0 Data Frame

```
SOF (15)
TYPE (0)
DST_WORLD_HI (0–15)
DST_WORLD_LO (0–15)
DST_PORT_HI_HI (0–15)
DST_PORT_HI_LO (0–15)
DST_PORT_LO_HI (0–15)
DST_PORT_LO_LO (0–15)
SRC_WORLD_HI (0–15)
SRC_WORLD_LO (0–15)
SRC_PORT_HI_HI (0–15)
SRC_PORT_HI_LO (0–15)
SRC_PORT_LO_HI (0–15)
SRC_PORT_LO_LO (0–15)
LEN_HI (0–15)
LEN_LO (0–15)
PAYLOAD[0..P-1]
EOF (0)
```

- Header = 15 nibbles (TYPE, DST_WORLD_HI, DST_WORLD_LO, DST_PORT_HI_HI, DST_PORT_HI_LO, DST_PORT_LO_HI, DST_PORT_LO_LO, SRC_WORLD_HI, SRC_WORLD_LO, SRC_PORT_HI_HI, SRC_PORT_HI_LO, SRC_PORT_LO_HI, SRC_PORT_LO_LO, LEN_HI, LEN_LO)
- Payload Length = (LEN_HI << 4) | LEN_LO = number of payload nibbles.
- Minimum valid Payload Length = 0.

---

### 3.0.0 Example

- **Destination world:** `0x00` → overworld
- **Destination port:** `0x34` → decimal `52`
- **Source world:** `0x00` → overworld
- **Source port:** `0x12` → decimal `18`
- **Payload:** `[0xA, 0xB]` → decimal `[10, 11]`

---

### Step 1. Payload Length

- Payload length = 2 nibbles

- `LEN_HI = 0x0` → decimal `0`

- `LEN_LO = 0x2` → decimal `2`

---

### Step 2. Addresses

- Destination world = `0x00` → `DST_WORLD_HI=0x0` (0), `DST_WORLD_LO=0x0` (0)
- Destination port = `0x0034` (52) → `DST_PORT_HI_HI=0x0` (0), `DST_PORT_HI_LO=0x0` (0), `DST_PORT_LO_HI=0x3` (3), `DST_PORT_LO_LO=0x4` (4)
- Source world = `0x00` → `SRC_WORLD_HI=0x0` (0), `SRC_WORLD_LO=0x0` (0)
- Source port = `0x0012` (18) → `SRC_PORT_HI_HI=0x0` (0), `SRC_PORT_HI_LO=0x0` (0), `SRC_PORT_LO_HI=0x1` (1), `SRC_PORT_LO_LO=0x2` (2)

---

### Step 3. Frame on the wire

| Symbol         | Hex | Decimal | Meaning                                         |
| -------------- | --- | ------- | ----------------------------------------------- |
| SOF            | 0xF | 15      | Start of Frame                                  |
| TYPE           | 0x0 | 0       | Frame Type (0 = Data)                           |
| DST_WORLD_HI   | 0x0 | 0       | Destination world high nibble                   |
| DST_WORLD_LO   | 0x0 | 0       | Destination world low nibble → 0x00 = overworld |
| DST_PORT_HI_HI | 0x0 | 0       | Destination port high-high nibble               |
| DST_PORT_HI_LO | 0x0 | 0       | Destination port high-low nibble                |
| DST_PORT_LO_HI | 0x3 | 3       | Destination port low-high nibble                |
| DST_PORT_LO_LO | 0x4 | 4       | Destination port low-low nibble → 0x0034 = 52   |
| SRC_WORLD_HI   | 0x0 | 0       | Source world high nibble                        |
| SRC_WORLD_LO   | 0x0 | 0       | Source world low nibble → 0x00 = overworld      |
| SRC_PORT_HI_HI | 0x0 | 0       | Source port high-high nibble                    |
| SRC_PORT_HI_LO | 0x0 | 0       | Source port high-low nibble                     |
| SRC_PORT_LO_HI | 0x1 | 1       | Source port low-high nibble                     |
| SRC_PORT_LO_LO | 0x2 | 2       | Source port low-low nibble → 0x0012 = 18        |
| LEN_HI         | 0x0 | 0       | Payload length high nibble                      |
| LEN_LO         | 0x2 | 2       | Payload length low nibble → Payload length = 2  |
| PAY0           | 0xA | 10      | Payload nibble 0                                |
| PAY1           | 0xB | 11      | Payload nibble 1                                |
| EOF            | 0x0 | 0       | End of Frame                                    |

---

### Step 4. Receiver interpretation

- Payload length = 2 nibbles
- Destination world = `0x00` → overworld
- Destination port = `0x0034` → 52
- Source world = `0x00` → overworld
- Source port = `0x0012` → 18
- Payload = `[0xA, 0xB]` → `[10, 11]`
- Frame terminates cleanly at EOF

---

### 3.2 Control Frame

Control frames are identified by TYPE = 1. The frame structure is:

```
SOF (15)
TYPE (1)
LEN_HI (0–15)
LEN_LO (0–15)
OP (0–14)
ARG[...] (LEN - 1 nibbles)
EOF (0)
```

Opcodes:

| Opcode | Mnemonic | Args        | Meaning                                                            |
| ------ | -------- | ----------- | ------------------------------------------------------------------ |
| 0x0    | NOP      | –           | Idle / resync assist                                               |
| 0x1    | RESET    | –           | Flush TX/RX, clear errors                                          |
| 0x2    | MODEQ    | –           | Request status frame                                               |
| 0x3    | SETPER   | 1 nibble    | Set symbol period (1–8, clamped)                                   |
| 0x4    | SETPORT  | 1–4 nibbles | Set port (high-high, high-low, low-high, low-low nibbles optional) |
| 0x5    | STATSCLR | –           | Clear counters                                                     |

---

### 3.2.0 Example Control Frame (Set Port)

---

### Target value

- Port = `0x002A` (42)
- High-high nibble = `0x0`
- High-low nibble = `0x0`
- Low-high nibble = `0x2`
- Low-low nibble = `0xA`

---

### Frame on the wire

```
15   (SOF)
1    (TYPE = 1 → Control)
0    (LEN_HI)
6    (LEN_LO = 6)
4    (OP = SETPORT)
0    (ARG[0] = high-high nibble = 0x0)
0    (ARG[1] = high-low nibble = 0x0)
2    (ARG[2] = low-high nibble = 0x2)
A    (ARG[3] = low-low nibble = 0xA)
0    (EOF)
```

---

### Breakdown

- **SOF (15):** Marks start of frame
- **TYPE = 1:** Control frame
- **LEN = 6:** Payload length = 6 nibbles (OP + 4 ARG)
- **OP = 4:** The SETPORT opcode
- **ARG[0] = 0x0:** High-high nibble of the port number
- **ARG[1] = 0x0:** High-low nibble of the port number
- **ARG[2] = 0x2:** Low-high nibble of the port number
- **ARG[3] = 0xA:** Low-low nibble of the port number
- **EOF (0):** Frame terminator

---

### Notes

- If you only wanted to set a port ≤ 15 (fits in one nibble), LEN = 2, omit `ARG[0]`, `ARG[1]`, `ARG[2]`.
- For example, port `0x7` would be:

  ```
  15, 1, 0,2, 4,7, 0
  ```

- For ports ≤ 255 (fits in two nibbles), LEN = 3, omit `ARG[0]`, `ARG[1]`.
- For ports ≤ 4095 (fits in three nibbles), LEN = 4, omit `ARG[0]`.

---

### 3.3 Status Frame (TYPE=2, LEN=8)

Status frames are requested via the MODEQ control frame but are not yet implemented in the current version.

Proposed format when implemented:

```
SOF (15)
TYPE (2)
LEN_HI (0)
LEN_LO (8)
[0]=0xA (signature nibble)
[1]=World high nibble
[2]=World low nibble
[3]=Port high nibble
[4]=Port low nibble
[5]=RX queue depth (0–13 clipped)
[6]=TX queue depth (0–13 clipped)
[7]=Error flags bitmap (bit0=RX_OVERFLOW, bit1=TX_FRAMING_ERR)
EOF (0)
```

---

### 3.4 To IPv4 Frame (TYPE=3)

Outbound frame from local NIC to remote IPv4 host. Includes source addressing and destination IP/port.

Structure:

```
SOF (15)
TYPE (3)
SRC_WORLD_HI (0–15)
SRC_WORLD_LO (0–15)
SRC_PORT_HI_HI (0–15)
SRC_PORT_HI_LO (0–15)
SRC_PORT_LO_HI (0–15)
SRC_PORT_LO_LO (0–15)
DST_IP_N0 (0–15)
DST_IP_N1 (0–15)
DST_IP_N2 (0–15)
DST_IP_N3 (0–15)
DST_IP_N4 (0–15)
DST_IP_N5 (0–15)
DST_IP_N6 (0–15)
DST_IP_N7 (0–15)
DST_PORT_HI_HI (0–15)
DST_PORT_HI_LO (0–15)
DST_PORT_LO_HI (0–15)
DST_PORT_LO_LO (0–15)
LEN_HI (0–15)
LEN_LO (0–15)
PAYLOAD[0..P-1] (LEN nibbles)
EOF (0)
```

- Header = 21 nibbles (TYPE, SRC_WORLD_HI, SRC_WORLD_LO, SRC_PORT_HI_HI, SRC_PORT_HI_LO, SRC_PORT_LO_HI, SRC_PORT_LO_LO, DST_IP_N0..N7, DST_PORT_HI_HI, DST_PORT_HI_LO, DST_PORT_LO_HI, DST_PORT_LO_LO, LEN_HI, LEN_LO)
- Payload Length = (LEN_HI << 4) | LEN_LO = number of payload nibbles.
- IPv4 address = 4 bytes (8 nibbles), high nibble first per byte.

Example: To 192.168.1.10 port 52 from world 0 port 18 with payload [10, 11]

```
SOF: 15
TYPE: 3
SRC_WORLD_HI: 0
SRC_WORLD_LO: 0
SRC_PORT_HI_HI: 0
SRC_PORT_HI_LO: 0
SRC_PORT_LO_HI: 1
SRC_PORT_LO_LO: 2
DST_IP_N0: C
DST_IP_N1: 0
DST_IP_N2: A
DST_IP_N3: 8
DST_IP_N4: 0
DST_IP_N5: 1
DST_IP_N6: 0
DST_IP_N7: A
DST_PORT_HI_HI: 0
DST_PORT_HI_LO: 0
DST_PORT_LO_HI: 3
DST_PORT_LO_LO: 4
LEN_HI: 0
LEN_LO: 2
PAYLOAD: A, B
EOF: 0
```

---

### 3.5 From IPv4 Frame (TYPE=4)

Inbound frame from remote IPv4 host to local NIC. Includes destination addressing and source IP/port.

Structure:

```
SOF (15)
TYPE (4)
DST_WORLD_HI (0–15)
DST_WORLD_LO (0–15)
DST_PORT_HI_HI (0–15)
DST_PORT_HI_LO (0–15)
DST_PORT_LO_HI (0–15)
DST_PORT_LO_LO (0–15)
SRC_IP_N0 (0–15)
SRC_IP_N1 (0–15)
SRC_IP_N2 (0–15)
SRC_IP_N3 (0–15)
SRC_IP_N4 (0–15)
SRC_IP_N5 (0–15)
SRC_IP_N6 (0–15)
SRC_IP_N7 (0–15)
SRC_PORT_HI_HI (0–15)
SRC_PORT_HI_LO (0–15)
SRC_PORT_LO_HI (0–15)
SRC_PORT_LO_LO (0–15)
LEN_HI (0–15)
LEN_LO (0–15)
PAYLOAD[0..P-1] (LEN nibbles)
EOF (0)
```

- Header = 21 nibbles (TYPE, DST_WORLD_HI, DST_WORLD_LO, DST_PORT_HI_HI, DST_PORT_HI_LO, DST_PORT_LO_HI, DST_PORT_LO_LO, SRC_IP_N0..N7, SRC_PORT_HI_HI, SRC_PORT_HI_LO, SRC_PORT_LO_HI, SRC_PORT_LO_LO, LEN_HI, LEN_LO)
- Payload Length = (LEN_HI << 4) | LEN_LO = number of payload nibbles.
- IPv4 address = 4 bytes (8 nibbles), high nibble first per byte.

Example: From 192.168.1.10 port 18 to world 0 port 52 with payload [10, 11]

```
SOF: 15
TYPE: 4
DST_WORLD_HI: 0
DST_WORLD_LO: 0
DST_PORT_HI_HI: 0
DST_PORT_HI_LO: 0
DST_PORT_LO_HI: 3
DST_PORT_LO_LO: 4
SRC_IP_N0: C
SRC_IP_N1: 0
SRC_IP_N2: A
SRC_IP_N3: 8
SRC_IP_N4: 0
SRC_IP_N5: 1
SRC_IP_N6: 0
SRC_IP_N7: A
SRC_PORT_HI_HI: 0
SRC_PORT_HI_LO: 0
SRC_PORT_LO_HI: 1
SRC_PORT_LO_LO: 2
LEN_HI: 0
LEN_LO: 2
PAYLOAD: A, B
EOF: 0
```

## 4. State Machines

### 4.1 Tx Framer (Host Redstone → Network Frame Dispatch)

```
IDLE: expect SOF=15. Else stay.
TYPE: read TYPE.
If TYPE=0: collect DST_WORLD/DST_WORLD/DST_PORT_HI_HI/DST_PORT_HI_LO/DST_PORT_LO_HI/DST_PORT_LO_LO/SRC_WORLD/SRC_WORLD/SRC_PORT_HI_HI/SRC_PORT_HI_LO/SRC_PORT_LO_HI/SRC_PORT_LO_LO (14 nibbles).
If TYPE=3: collect SRC_WORLD/SRC_WORLD/SRC_PORT_HI_HI/SRC_PORT_HI_LO/SRC_PORT_LO_HI/SRC_PORT_LO_LO/DST_IP_N0..N7/DST_PORT_HI_HI/DST_PORT_HI_LO/DST_PORT_LO_HI/DST_PORT_LO_LO (20 nibbles).
If TYPE=4: collect DST_WORLD/DST_WORLD/DST_PORT_HI_HI/DST_PORT_HI_LO/DST_PORT_LO_HI/DST_PORT_LO_LO/SRC_IP_N0..N7/SRC_PORT_HI_HI/SRC_PORT_HI_LO/SRC_PORT_LO_HI/SRC_PORT_LO_LO (20 nibbles).
LEN: read LEN_HI + LEN_LO → Payload Length.
DATA: collect payload (LEN nibbles). If SOF before done → ERROR.
EOF: require 0. Else → ERROR. On success → COMMIT.
ERROR: increment TX_FRAMING_ERR, wait one quiet symbol (0), then return IDLE.
```

Overflow policy: if TX ring full at COMMIT → drop frame, increment TX_FRAMES_DROPPED counter.

---

### 4.2 Rx Emitter (Received Frame → Host Redstone Symbols)

```
If RX empty: output 0.
Else: output SOF, TYPE, [addresses if TYPE=0], LEN, payload, then EOF.
Advance every symbol_period_ticks.
```

---

## 5. Flow Control

- drop-on-overflow only (with counter).

---

## 6. Counters & Telemetry

Currently implemented:

- txFramesParsed
- txFramesDropped
- rxFramesEmitted
- txFramingErrors

Planned for future implementation:

- rxOverflowDrops
- unroutableFrames
- loopbackFrames

---

## 7. Configuration Knobs

| Name                | Storage | Default | Range   | Purpose                             |
| ------------------- | ------- | ------- | ------- | ----------------------------------- |
| symbol_period_ticks | NBT     | 2       | 1–8     | Timing tolerance (ticks per symbol) |
| port                | NBT     | 0       | 0–65535 | NIC's own port ID for frames        |

---

## 8. Error Codes / Flags

Bitfield:

- bit0 RX_OVERFLOW
- bit1 TX_FRAMING_ERR

---

## 9. Design Decisions

| Aspect                 | Decision                                                                        |
| ---------------------- | ------------------------------------------------------------------------------- |
| EOF Handling           | Exactly one 0 nibble terminates frame. Idle = constant 0.                       |
| Max Frame Length       | 255 payload nibbles (LEN=255)                                                   |
| Frame Types            | 0=Data (with addresses), 1=Control, 2=Status (proposed), 3=To IPv4, 4=From IPv4 |
| Symbol Period          | Default 2 ticks, range 1–8                                                      |
| Overflow               | Drop newest frame, increment counter                                            |
| Status Frame Signature | First nibble = 0xA                                                              |
| Port Header            | DST_WORLD + DST_PORT (16 bits) + SRC_WORLD + SRC_PORT (16 bits)                 |
| RESET Behavior         | Flush TX, RX, clear error flags (counters remain unless STATSCLR)               |
| Noise Recovery         | Invalid sequence → ERROR → require ≥1 idle nibble (0) → IDLE                    |
