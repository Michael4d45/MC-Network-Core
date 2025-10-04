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

| Layer | Real NIC Equivalent | NetworkCore Responsibility                   |
| ----- | ------------------- | -------------------------------------------- |
| L1    | Physical            | Redstone power levels (0–15) sampled/emitted |
| L2    | Data Link (MAC)     | Frame delimiting, length                     |
| L3+   | Network / Transport | Port-based virtual channels, routing         |

---

## 3. Wire / Symbol Protocol

### 3.0 Data Frame

```
SOF (15)
TYPE (0)
DST_WORLD (2x0–15)
DST_PORT (4x0–15)
SRC_WORLD (2x0–15)
SRC_PORT (4x0–15)
LEN (2x0–15)
PAYLOAD[0..P-1]
EOF (0)
```

- Header = 15 nibbles (TYPE + 14 data nibbles)
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

| Field     | Nibbles | Hex                | Decimal    | Meaning                                          |
| --------- | ------- | ------------------ | ---------- | ------------------------------------------------ |
| SOF       | -       | 0xF                | 15         | Start of Frame                                   |
| TYPE      | 0       | 0x0                | 0          | Frame Type (0 = Data)                            |
| DST_WORLD | 1-2     | 0x0, 0x0           | 0, 0       | Destination world (2 nibbles) → 0x00 = overworld |
| DST_PORT  | 3-6     | 0x0, 0x0, 0x3, 0x4 | 0, 0, 3, 4 | Destination port (4 nibbles) → 0x0034 = 52       |
| SRC_WORLD | 7-8     | 0x0, 0x0           | 0, 0       | Source world (2 nibbles) → 0x00 = overworld      |
| SRC_PORT  | 9-12    | 0x0, 0x0, 0x1, 0x2 | 0, 0, 1, 2 | Source port (4 nibbles) → 0x0012 = 18            |
| LEN       | 13-14   | 0x0, 0x2           | 0, 2       | Payload length (2 nibbles) → length = 2          |
| PAYLOAD   | 15-16   | 0xA, 0xB           | 10, 11     | Payload nibbles                                  |
| EOF       | -       | 0x0                | 0          | End of Frame                                     |

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
OP (0–14)
LEN_HI (0–15)
LEN_LO (0–15)
ARG[...] (LEN nibbles)
EOF (0)
```

Opcodes:

| Opcode | Mnemonic | Args        | Meaning                                                            |
| ------ | -------- | ----------- | ------------------------------------------------------------------ |
| 0x0    | NOP      | –           | Idle / resync assist                                               |
| 0x1    | RESET    | –           | Flush TX/RX, clear errors                                          |
| 0x2    | MODEQ    | –           | Request status frame                                               |
| 0x3    | SETPORT  | 1–4 nibbles | Set port (high-high, high-low, low-high, low-low nibbles optional) |
| 0x4    | STATSCLR | –           | Clear counters                                                     |

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
4    (OP = SETPORT)
0    (LEN_HI)
4    (LEN_LO = 4)
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
- **OP = 4:** The SETPORT opcode
- **LEN = 4:** ARG length = 4 nibbles
- **ARG[0] = 0x0:** High-high nibble of the port number
- **ARG[1] = 0x0:** High-low nibble of the port number
- **ARG[2] = 0x2:** Low-high nibble of the port number
- **ARG[3] = 0xA:** Low-low nibble of the port number
- **EOF (0):** Frame terminator

---

### 3.3 Status Frame (TYPE=2, LEN=8)

Status frames are requested via the MODEQ control frame.

Format:

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

### 3.4 IPv4 Frame (TYPE=3)

Inbound and outbound frame between local NIC and remote IPv4 host.

Structure:

```
SOF (15)
TYPE (3)
DST_IP (8x0–15)
DST_UDP_PORT (4x0–15)
DST_WORLD (2x0–15)
DST_PORT (4x0–15)
SRC_IP (8x0–15)
SRC_UDP_PORT (4x0–15)
SRC_WORLD (2x0–15)
SRC_PORT (4x0–15)
LEN (2x0–15)
PAYLOAD[0..P-1] (LEN nibbles)
EOF (0)
```

- Header = 39 nibbles (TYPE + 38 data nibbles)
- Payload Length = (LEN_HI << 4) | LEN_LO = number of payload nibbles.
- IPv4 address = 4 bytes (8 nibbles), high nibble first per byte.
- Direction determined by context: outbound when sent from redstone, inbound when received from IPv4Router.

Example: IPv4 frame from 192.168.1.10:18 to world 0 port 52 UDP port 0 with payload [10, 11]

```
SOF: 15
TYPE: 3
DST_IP: C, 0, A, 8, 0, 1, 0, A
DST_UDP_PORT: 0, 0, 0, 0
DST_WORLD: 0, 0
DST_PORT: 0, 0, 3, 4
SRC_IP: 0, 0, 0, 0, 0, 0, 0, 0
SRC_UDP_PORT: 0, 0, 0, 0
SRC_WORLD: 0, 0
SRC_PORT: 0, 0, 1, 2
LEN: 0, 2
PAYLOAD: A, B
EOF: 0
```

## 4. State Machines

### 4.1 Tx Framer (Host Redstone → Network Frame Dispatch)

```
IDLE: expect SOF=15. Else stay.
TYPE: read TYPE.
If TYPE=0: collect 14 nibbles (DST_WORLD, DST_PORT, SRC_WORLD, SRC_PORT, LEN).
If TYPE=3: collect 38 nibbles (DST_IP, DST_UDP_PORT, DST_WORLD, DST_PORT, SRC_IP, SRC_UDP_PORT, SRC_WORLD, SRC_PORT, LEN).
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
Advance when clock is powered.
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

| Name | Storage | Default | Range   | Purpose                      |
| ---- | ------- | ------- | ------- | ---------------------------- |
| port | NBT     | 0       | 0–65535 | NIC's own port ID for frames |

---

## 8. Error Codes / Flags

Bitfield:

- bit0 RX_OVERFLOW
- bit1 TX_FRAMING_ERR

---

## 9. Design Decisions

| Aspect | Decision |
| --- | --- |
| EOF Handling | Exactly one 0 nibble terminates frame. Idle = constant 0. |
| Max Frame Length | 255 payload nibbles (LEN=255) |
| Frame Types | 0=Data (with addresses), 1=Control, 2=Status, 3=IPv4 |
| Overflow | Drop newest frame, increment counter |
| Status Frame Signature | First nibble = 0xA |
| Port Header | DST_WORLD + DST_PORT (16 bits) + SRC_WORLD + SRC_PORT (16 bits) for Data frames; IPv4 frames include additional UDP ports |
| RESET Behavior | Flush TX, RX, clear error flags (counters remain unless STATSCLR) |
| Noise Recovery | Invalid sequence → ERROR → require ≥1 idle nibble (0) → IDLE |
