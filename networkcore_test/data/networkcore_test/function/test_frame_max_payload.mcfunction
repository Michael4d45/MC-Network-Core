# Max length data frame test with 255 nibble payload
# SOF=15 (Start of Frame), TYPE=0 (Data), DST_WORLD_HI=0, DST_WORLD_LO=0 (World=0), DST_PORT_HI_HI=0, DST_PORT_HI_LO=0, DST_PORT_LO_HI=2, DST_PORT_LO_LO=10 (Port=42), SRC_WORLD_HI=0, SRC_WORLD_LO=0 (World=0), SRC_PORT_HI_HI=0, SRC_PORT_HI_LO=0, SRC_PORT_LO_HI=1, SRC_PORT_LO_LO=2 (Port=18), LEN_HI=15, LEN_LO=15 (Length=255), PAYLOAD[0..254], EOF=0 (End of Frame)
# DST_PORT = 42, SRC_PORT = 18, PAYLOAD = [255 nibbles: repeating pattern 1-14]
# LEN = 0xFB (251) → payload length = 251 nibbles
# Pattern: repeating 1..14 (skips 0 and 15 to avoid EOF/SOF mid-payload) then wraps
# Ports: DST=0x002A (0,0,2,A) SRC=0x0012 (0,0,1,2)
# Worlds: DST=0x00 SRC=0x00 (overworld)
# Frame layout:
# SOF 15
# TYPE 0
# DST_WORLD 0 0
# DST_PORT 0 0 2 10
# SRC_WORLD 0 0
# SRC_PORT 0 0 1 2
# LEN 15 15
# PAYLOAD (255 nibbles pattern)
# EOF 0

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 0
# DST_WORLD
networkcore sendtest 0
networkcore sendtest 0
# DST_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# SRC_WORLD
networkcore sendtest 0
networkcore sendtest 0
# SRC_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
# LEN
networkcore sendtest 15
networkcore sendtest 15
# PAYLOAD (repeating pattern 1-14)
# Generated pattern below
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 14
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 28
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 42
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 56
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 70
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 84
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 98
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 112
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 126
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 140
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 154
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 168
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 182
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 196
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 210
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 224
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 238
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 12
networkcore sendtest 13
networkcore sendtest 14
# 252
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
# 255
networkcore sendtest 0
