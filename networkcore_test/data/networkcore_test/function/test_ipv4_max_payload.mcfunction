# Test IPv4 frame with max payload (255 nibbles) to 192.168.1.100:12345 from world 0 port 18
# SOF=15 (Start of Frame), TYPE=3 (To IPv4), SRC_WORLD_HI=0, SRC_WORLD_LO=0 (World=0), SRC_PORT_HI_HI=0, SRC_PORT_HI_LO=0, SRC_PORT_LO_HI=1, SRC_PORT_LO_LO=2 (Port=18), DST_IP_N0=12, N1=0, N2=10, N3=8, N4=0, N5=1, N6=6, N7=4 (192.168.1.100), DST_PORT_HI_HI=3, HI_LO=0, LO_HI=3, LO_LO=9 (Port=12345), LEN_HI=15, LEN_LO=15 (Length=255), PAYLOAD[0..254], EOF=0 (End of Frame)
# DST_IP = 192.168.1.100, DST_PORT = 12345, SRC_WORLD = 0, SRC_PORT = 18, PAYLOAD = [255 nibbles: repeating pattern 1-14]
# LEN = 0xFF (255) → payload length = 255 nibbles
# Pattern: repeating 1..14 (skips 0 and 15 to avoid EOF/SOF mid-payload) then wraps
# Ports: DST=0x3039 (3,0,3,9) SRC=0x0012 (0,0,1,2)
# Worlds: SRC=0x00 (overworld)
# IP: 192.168.1.100 = C0:A8:01:64 → nibbles C,0,A,8,0,1,6,4
# Frame layout:
# SOF 15
# TYPE 3
# SRC_WORLD_HI 0
# SRC_WORLD_LO 0
# SRC_PORT_HI_HI 0
# SRC_PORT_HI_LO 0
# SRC_PORT_LO_HI 1
# SRC_PORT_LO_LO 2
# DST_IP_N0 12
# DST_IP_N1 0
# DST_IP_N2 10
# DST_IP_N3 8
# DST_IP_N4 0
# DST_IP_N5 1
# DST_IP_N6 6
# DST_IP_N7 4
# DST_PORT_HI_HI 3
# DST_PORT_HI_LO 0
# DST_PORT_LO_HI 3
# DST_PORT_LO_LO 9
# LEN_HI 15
# LEN_LO 15
# PAYLOAD (255 nibbles pattern)
# EOF 0

networkcore pauseTickProcess
networkcore sendtest 15
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 6
networkcore sendtest 4
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
networkcore sendtest 15
networkcore sendtest 15
# Payload start (255 symbols)
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
networkcore resumeTickProcess
