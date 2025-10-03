# Test IPv4 frame with max payload (255 nibbles) to 192.168.1.161:12345 from world 0 port 18
# SOF=15 (Start of Frame), TYPE=3 (IPv4), DST_IP=12,0,10,8,0,1,10,1 (192.168.1.161), DST_UDP_PORT=3,0,3,9 (12345), DST_WORLD=0,0 (0), DST_PORT=0,0,2,10 (42), SRC_IP=7,15,0,0,0,0,0,1 (127.0.0.1), SRC_UDP_PORT=3,0,3,9 (12345), SRC_WORLD=0,0 (0), SRC_PORT=0,0,1,2 (18), LEN=15,15 (255), PAYLOAD[0..254], EOF=0 (End of Frame)
# DST_IP = 192.168.1.161, DST_UDP_PORT = 12345, SRC_PORT = 18, PAYLOAD = [255 nibbles: repeating pattern 1-14]
# LEN = 0xFF (255) → payload length = 255 nibbles
# Pattern: repeating 1..14 (skips 0 and 15 to avoid EOF/SOF mid-payload) then wraps
# Frame layout:
# SOF 15
# TYPE 3
# DST_IP 12,0,10,8,0,1,10,1 (192.168.1.161)
# DST_UDP_PORT 3,0,3,9 (12345)
# DST_WORLD 0,0 (0)
# DST_PORT 0,0,2,10 (42)
# SRC_IP 7,15,0,0,0,0,0,1 (127.0.0.1)
# SRC_UDP_PORT 3,0,3,9 (12345)
# SRC_WORLD 0,0 (0)
# SRC_PORT 0,0,1,2 (18)
# LEN 15,15 (255)
# PAYLOAD (255 nibbles pattern)
# EOF 0

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 3
# DST_IP
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 10
networkcore sendtest 1
# DST_UDP_PORT
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
# DST_WORLD
networkcore sendtest 0
networkcore sendtest 0
# DST_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# SRC_IP
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
networkcore sendtest 0
networkcore sendtest 1
# SRC_UDP_PORT
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
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
# EOF
networkcore sendtest 0
