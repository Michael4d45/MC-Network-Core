# Minecraft NIC / Router Network Protocol Specification

### Table of Contents

1. [Overview](#overview)
2. [Frame Structure](#frame-structure)

   - [2.1 Nibble-Level Framing](#21-nibble-level-framing)

3. [Frame Types](#frame-types)

   - [3.1 Data Frames](#31-data-frames)
   - [3.1.1 Data Control Frames](#311-data-control-frame--layout)
   - [3.2 IPv4 Frames](#32-ipv4-frames)
   - [3.3 Frame Hierarchy & Encapsulation Diagram](#33-frame-hierarchy--encapsulation-diagram)
   - [3.4 Component Responsibilities](#34-component-responsibilities)
   - [3.5 Diagnostic Flow Examples](#35-diagnostic-flow-examples)
   - [3.6 Layer Interaction Summary](#36-layer-interaction-summary)

4. [Developer Quick Reference](#4-developer-quick-reference)

---

## Overview

This protocol defines a simple digital communication system for **Minecraft** NIC and router blocks. It allows for:

- Intra-world communication between blocks (`Data` and `Data Control` frames).
- Inter-world or inter-instance communication over IPv4 (`IPv4` frames encapsulating Data/Data Control).
- Status and diagnostic responses (via `Data Control` frames with status payload).

Routers and NICs forward or generate control frames in response to delivery failures or timeouts. All diagnostics and error reporting use Data Control frames, whether local or remote (encapsulated in IPv4 for cross-instance delivery).

### Clock Gating

Network Core blocks support **optional clock gating** for deterministic symbol timing:

- **Clock faces:** Any of the four orthogonal sides (excluding the transmit and receive faces)
- **Behavior:** Symbol processing occurs only when at least one clock face receives redstone power > 0 (level-triggered)
- **Tick rate:** Block entity ticks every 2 game ticks (aligned with redstone tick timing)
- **Use case:** External clock circuits can control frame transmission timing without relying on continuous operation

---

## Frame Structure

All communication is **nibble-based** (0–15), transmitted via redstone power levels. Each frame begins with `SOF = 15` and ends with `EOF = 0`.

| Field       | Size (nibbles) | Description                   |
| ----------- | -------------- | ----------------------------- |
| SOF         | 1              | Start of Frame (`15`)         |
| TYPE        | 1              | Frame type (0–15)             |
| CODE        | 1              | Subtype or control code       |
| LEN_HI      | 1              | High nibble of payload length |
| LEN_LO      | 1              | Low nibble of payload length  |
| ARG[0..N-1] | variable       | Payload / arguments           |
| EOF         | 1              | End of Frame (`0`)            |

---

## 2.1 Nibble-Level Framing

Each frame is encoded entirely in nibbles (4-bit values). Transmitters pulse redstone levels per tick in `[0–15]`, with `15` marking SOF and `0` marking EOF. Routers buffer until EOF to ensure complete frame delivery.

---

## 3.1 Data Frames

- **TYPE = 0**
- Used for normal data transfer between blocks in the same world.
- Routed via port numbers on each block.
- **CODE field**: **Must be 0x0** for all standard Data frames in protocol v1. Non-zero values (0x1–0xF) are **reserved** for future application-layer extensions (fragmentation, priority, etc.) and must not be used.
- **LEN field**: Specifies the **total ARG length** (includes 8 port nibbles + payload).
- **Maximum payload**: 247 nibbles (because total args = 8 ports + 247 payload = 255 max).

| Field      | Nibbles | Description                      |
| ---------- | ------- | -------------------------------- |
| `DST_PORT` | 4       | Destination port (0x0000–0xFFFF) |
| `SRC_PORT` | 4       | Source port (0x0000–0xFFFF)      |
| `PAYLOAD`  | 0–247   | Application data                 |

**Important**: The LEN field encodes the **total args length** (8 port nibbles + payload nibbles), consistent with all other frame types. Maximum LEN value is 0xFF (255), allowing up to 247 nibbles of payload after accounting for the 8 port nibbles.

---

## 3.1.1 Data Control Frame — Layout

- **TYPE = 1**
- Used for intra-world control and diagnostics between blocks.

```
SOF (15)
TYPE (1)
CODE (0–15)
LEN_HI (0–15)
LEN_LO (0–15)
ARG[0..N-1]
EOF (0)
```

### Example — Port Unreachable (CODE = 0x1)

| Field    | Nibbles | Hex     | Meaning               |
| -------- | ------- | ------- | --------------------- |
| SOF      | –       | F       | Start of Frame        |
| TYPE     | –       | 1       | Data Control Frame    |
| CODE     | –       | 1       | Port Unreachable      |
| LEN_HI   | –       | 0       |                       |
| LEN_LO   | –       | 4       | Four argument nibbles |
| ARG[0–3] | 4       | 0 0 3 4 | Port = 0x0034         |
| EOF      | –       | 0       | End of Frame          |

### Example — DataPing (CODE = 0x4)

| Field    | Nibbles | Hex | Meaning              |
| -------- | ------- | --- | -------------------- |
| SOF      | –       | F   | Start of Frame       |
| TYPE     | –       | 1   | Data Control         |
| CODE     | –       | 4   | ECHO_REQUEST         |
| LEN_HI   | –       | 0   |                      |
| LEN_LO   | –       | 2   | Two argument nibbles |
| ARG[0–1] | 2       | A B | Echo payload `[A,B]` |
| EOF      | –       | 0   | End of Frame         |

Reply uses **CODE=0x5** (ECHO_REPLY) with identical payload.

### Example — SetPort (CODE = 0x8)

| Field    | Nibbles | Hex     | Meaning                      |
| -------- | ------- | ------- | ---------------------------- |
| SOF      | –       | F       | Start of Frame               |
| TYPE     | –       | 1       | Data Control Frame           |
| CODE     | –       | 8       | SETPORT                      |
| LEN_HI   | –       | 0       |                              |
| LEN_LO   | –       | 4       | Four argument nibbles (port) |
| ARG[0–3] | 4       | 0 0 4 2 | Requested port = 0x0042      |
| EOF      | –       | 0       | End of Frame                 |

On receipt the core reconciles the requested port with the allocator. If a conflict forces a different assignment the follow-up STATUS_REPLY reflects the actual port now bound to the block.

### Example — Status Reply (CODE = 0x9)

| Field      | Nibbles | Hex             | Meaning                     |
| ---------- | ------- | --------------- | --------------------------- |
| SOF        | –       | F               | Start of Frame              |
| TYPE       | –       | 1               | Data Control Frame          |
| CODE       | –       | 9               | STATUS_REPLY                |
| LEN_HI     | –       | 1               |                             |
| LEN_LO     | –       | 4               | 20 argument nibbles         |
| ARG[0]     | 1       | A               | Signature                   |
| ARG[1–4]   | 4       | 0 0 4 2         | Port = 0x0042               |
| ARG[5–6]   | 2       | 0 3             | RX queue depth = 3          |
| ARG[7]     | 1       | 0               | Error flags = 0 (no errors) |
| ARG[8–15]  | 8       | C 0 A 8 0 1 0 A | IPv4 address = 192.168.1.10 |
| ARG[16–19] | 4       | 3 0 3 9         | UDP port = 0x3039 (12345)   |
| EOF        | –       | 0               | End of Frame                |

STATUS_REPLY is emitted in response to MODEQ requests. Payload contains:

- **Signature** (0xA): Frame identifier
- **Port** (4 nibbles): Current port assignment
- **RX queue depth** (2 nibbles): Frames pending emission (0–255, clamped to max 64)
- **Error flags** (1 nibble): Bit0=RX_OVERFLOW, Bit1=TX_FRAMING_ERR, Bit2=PORT_ALLOC_FAILURE (reserved), Bit3=IPV4_ROUTING_FAILURE (reserved)
- **IPv4 address** (8 nibbles): Server's IPv4 address for IPv4 routing
- **UDP port** (4 nibbles): UDP port used for IPv4 routing

---

### Data Control Codes

| Code | Mnemonic         | Args       | Notes                                               |
| ---- | ---------------- | ---------- | --------------------------------------------------- |
| 0x0  | NOP              | none       | Keepalive                                           |
| 0x1  | PORT_UNREACHABLE | 4 nibbles  | Block port closed                                   |
| 0x2  | MALFORMED_FRAME  | none       | **Accepted but not emitted** (future use)           |
| 0x3  | BLOCK_BUSY       | 4 nibbles  | Target busy                                         |
| 0x4  | ECHO_REQUEST     | variable   | Ping request                                        |
| 0x5  | ECHO_REPLY       | variable   | Ping response                                       |
| 0x6  | MODEQ            | none       | Request status                                      |
| 0x7  | RESET            | none       | Reset queues                                        |
| 0x8  | SETPORT          | 4 nibbles  | Request port reassignment                           |
| 0x9  | STATUS_REPLY     | 20 nibbles | Status response (port, queue, errors, IP, UDP port) |
| 0xA  | HOST_UNREACHABLE | 8 nibbles  | Remote instance offline (IP address)                |
| 0xB  | NETWORK_ERROR    | variable   | Network routing failure                             |
| 0xC  | TARGET_BUSY      | 4 nibbles  | Remote target queue full                            |

**Note on MALFORMED_FRAME**: This control frame is **accepted but not emitted** by the implementation. Receivers should handle incoming MALFORMED_FRAME notifications gracefully (currently logged). Framing errors encountered during TX parsing increment the error counter (visible in STATUS_REPLY via bit1 of error flags) but do not trigger outgoing MALFORMED_FRAME responses. This is intentional to avoid feedback loops and excessive control traffic from garbled signals. Reserved for future use.

---

## 3.2 IPv4 Frames

- **TYPE = 3**
- Used for inter-instance (network) transport via the router.
- Contain IPv4-style source/destination addresses and UDP-like ports.
- **CODE field**: Reserved for future application-layer use. **Should be 0x0** for all standard IPv4 frames in protocol v1. Non-zero values (0x1–0xF) may be used by future extensions to indicate priority, QoS flags, or transport metadata. Current implementation accepts but does not interpret non-zero CODE values.
- **Encapsulation**: IPv4 frames encapsulate **Data (TYPE=0)** or **Data Control (TYPE=1)** frames only.
- **LEN field**: Total nibbles in payload = 24 (addressing) + 4 (inner header) + inner payload length.
- IPv4 frames **cannot** encapsulate other IPv4 frames.

**Payload Structure (28 + inner payload nibbles):**

| Field           | Nibbles | Description                        |
| --------------- | ------- | ---------------------------------- |
| `DST_IP`        | 8       | Destination IPv4 address           |
| `DST_UDP_PORT`  | 4       | Destination UDP port               |
| `SRC_IP`        | 8       | Source IPv4 address                |
| `SRC_UDP_PORT`  | 4       | Source UDP port                    |
| `INNER_TYPE`    | 1       | Encapsulated frame type (0 or 1)   |
| `INNER_CODE`    | 1       | Encapsulated frame code            |
| `INNER_LEN_HI`  | 1       | Encapsulated payload length (high) |
| `INNER_LEN_LO`  | 1       | Encapsulated payload length (low)  |
| `INNER_PAYLOAD` | varies  | Encapsulated frame arguments       |

---

## 3.3 Frame Hierarchy & Encapsulation Diagram

```
                          ┌─────────────────────────────┐
                          │  Redstone / Physical Layer  │
                          └─────────────┬───────────────┘
                                        │
                                        ▼
                      ┌───────────────────────────────────┐
                      │  L2: Data Link Framing (SOF/EOF)  │
                      └─────────────────┬─────────────────┘
                                        │
             ┌──────────────────────────┴───────────────────────────────┐
             │                                                          │
             ▼                                                          ▼
┌──────────────────────────┐                               ┌──────────────────────────┐
│ TYPE=0  Data Frame       │                               │ TYPE=1  Data Control     │
│ • Intra-world message    │                               │ • Errors, pings, status  │
│ • Routed via port number │                               │ • Generated locally      │
└────────────┬─────────────┘                               │ • STATUS_REPLY payload   │
             │                                             └────────────┬─────────────┘
             │                                                          │
             │                        ┌─────────────────────────────────┘
             │                        │
             ▼                        ▼
┌──────────────────────────────────────────────────────────────────────┐
│ TYPE=3  IPv4 Frame                                                   │
│ • Wraps Data or Data Control frames                                  │
│ • Cross-instance routing via UDP                                     │
│ • Remote diagnostics use encapsulated Data Control (HOST_UNREACHABLE,│
│   NETWORK_ERROR, TARGET_BUSY, ECHO_REQUEST/REPLY, MODEQ)             │
└───────────────────────────┬──────────────────────────────────────────┘
                            │
                            ▼
                 ┌──────────────────────────┐
                 │ External UDP Proxy       │
                 │ • Converts IPv4↔UDP      │
                 │ • Forwards between games │
                 └──────────────────────────┘
```

### Encapsulation Rules

| Parent       | Child            | Notes                                           |
| ------------ | ---------------- | ----------------------------------------------- |
| IPv4 Frame   | Data             | Cross-instance data transfer                    |
| IPv4 Frame   | Data Control     | Cross-instance diagnostics & error reporting    |
| Data Frame   | Application data | Intra-instance only                             |
| Data Control | Status payload   | STATUS_REPLY provides port, queue depth, errors |

**Note**: IPv4 frames **cannot** encapsulate other IPv4 frames. Attempting to do so will result in a parse error.

---

## 3.4 Component Responsibilities

| Component | Handles | Generates | Forwards | Notes |
| --- | --- | --- | --- | --- |
| **NetworkCoreBlock (NIC)** | Data, Data Control | Data Control (Ping, Reset, Status, Errors) | No | Local & remote comms via Data frames |
| **IPv4Router (Proxy)** | IPv4 | IPv4-encapsulated Data Control errors | Yes | Bridges instances |
| **Redstone Wire** | SOF/EOF encoding | None | None | Physical signaling |
| **External UDP Host** | IPv4 payloads | IPv4 Frames | N/A | Network link |

---

## 3.5 Diagnostic Flow Examples

### Local Ping (DataPing)

```
Block A ── Data Control (ECHO_REQUEST) ──► Block B
Block B ── Data Control (ECHO_REPLY) ──► Block A
```

### Remote Ping (via IPv4)

```
NIC A ── IPv4(Data Control ECHO_REQUEST) ──► IPv4Router ──► NIC B
NIC B ── IPv4(Data Control ECHO_REPLY) ──► IPv4Router ──► NIC A
```

### Routing Failure

```
NIC A ── IPv4 Frame (to unreachable IP)
        ▼
IPv4Router detects no route
        ▼
IPv4Router ── IPv4(Data Control HOST_UNREACHABLE) ──► NIC A
```

### Status Request

```
Block A ── Data Control (MODEQ) ──► Block B
Block B ── Data Control (STATUS_REPLY with 20-nibble payload) ──► Block A
```

---

## 3.6 Layer Interaction Summary

| Layer          | Frame Types  | Typical Errors        | Control Type |
| -------------- | ------------ | --------------------- | ------------ |
| Data (local)   | 0, 1         | Port/host unreachable | Data Control |
| IPv4 (network) | 3            | Delivery via encap    | Data Control |
| Status / Debug | 1 (code 0x9) | –                     | STATUS_REPLY |

---

## 3.9 Flow Control & Queue Management

### RX Queue Capacity

Each Network Core maintains an **RX queue** with a capacity of **64 frames**. This queue buffers frames awaiting emission on the receive (output) side.

- **Queue overflow behavior**:

  - **Local senders** (Data frames): Receive `BLOCK_BUSY` (Data Control code 0x3) with target port
  - **IPv4 senders** (IPv4 frames): Receive `TARGET_BUSY` (Data Control code 0xC, encapsulated in IPv4 frame) with target port
  - Overflow increments the `rxOverflowDrops` counter and sets error flag bit0 (RX_OVERFLOW) in STATUS_REPLY

- **Frames competing for queue space**:

  - Data frames (TYPE=0)
  - Data Control frames (TYPE=1)
  - Encapsulated frames delivered via IPv4 (TYPE=3)

- **TX queue**: Not implemented. Transmit-side symbol parsing operates directly without buffering parsed frames.

**Design rationale**: The 64-frame capacity balances responsiveness with memory overhead. Applications requiring higher throughput should implement application-layer flow control or rate limiting.

### Frame Processing Order

- **Multiple frames in same tick**: IPv4-delivered frames are queued via `DataRouter.server.execute()` to ensure main-thread processing. All frames are handled sequentially on the server thread.
- **FIFO ordering**: Frames are processed in first-in, first-out order from the RX queue.
- **No priority**: All frame types (Data, Data Control, IPv4-encapsulated) share the same queue with equal priority.

---

## 4. Developer Quick Reference

| Event                | Send Frame     | CODE | Description                             |
| -------------------- | -------------- | ---- | --------------------------------------- |
| Ping (local)         | Data Control   | 0x4  | ECHO_REQUEST                            |
| Ping (remote)        | IPv4(Data Ctl) | 0x4  | ECHO_REQUEST encapsulated               |
| Ping reply           | Matching type  | 0x5  | ECHO_REPLY                              |
| No port listening    | Data Control   | 0x1  | PORT_UNREACHABLE                        |
| Invalid frame        | Data Control   | 0x2  | MALFORMED_FRAME (accepted, not emitted) |
| Host unreachable     | Data Control   | 0xA  | HOST_UNREACHABLE (8 nibbles IP)         |
| Network error        | Data Control   | 0xB  | NETWORK_ERROR (variable args)           |
| Target busy (local)  | Data Control   | 0x3  | BLOCK_BUSY                              |
| Target busy (remote) | Data Control   | 0xC  | TARGET_BUSY (encapsulated in IPv4)      |
| Status request       | Data Control   | 0x6  | MODEQ                                   |
| Status response      | Data Control   | 0x9  | STATUS_REPLY (20-nibble payload)        |
| Reset                | Data Control   | 0x7  | RESET                                   |
| Reassign port        | Data Control   | 0x8  | SETPORT                                 |
