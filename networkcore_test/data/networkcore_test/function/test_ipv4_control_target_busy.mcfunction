# IPv4 Control frame: TARGET_BUSY (TYPE=4, CODE=0x7)
# Layout: SOF, TYPE, CODE, LEN=0x14 (16 nibbles header + 4 port context), dst/src IP header, port, EOF

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 4
# CODE (TARGET_BUSY)
networkcore sendtest 7
# LEN (0x14 → 20 nibbles)
networkcore sendtest 1
networkcore sendtest 4
# DST_IP (127.0.0.1 → 7,15,0,0,0,0,0,1)
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
# SRC_IP (192.168.1.1 → 12,0,10,8,0,1,0,1)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
# CONTEXT: busy port 0x0042 → 0,0,4,2
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 4
networkcore sendtest 2
# EOF
networkcore sendtest 0
