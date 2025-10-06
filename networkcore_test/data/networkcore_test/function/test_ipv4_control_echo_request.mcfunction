# IPv4 Control frame: ECHO_REQUEST (TYPE=4, CODE=0x3) with payload [0xA,0xB]
# Layout: SOF, TYPE, CODE, LEN=0x12 (16 nibbles header + 2 payload), dst/src IP header, payload, EOF

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 4
# CODE (ECHO_REQUEST)
networkcore sendtest 3
# LEN (0x12 → 18 nibbles)
networkcore sendtest 1
networkcore sendtest 2
# DST_IP (127.0.0.1 → 7,15,0,0,0,0,0,1)
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
# SRC_IP (192.168.1.10 → 12,0,10,8,0,1,0,10)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 10
# PAYLOAD (0x0A, 0x0B)
networkcore sendtest 10
networkcore sendtest 11
# EOF
networkcore sendtest 0
