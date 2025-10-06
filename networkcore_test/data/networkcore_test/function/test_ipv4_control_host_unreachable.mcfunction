# IPv4 Control frame: HOST_UNREACHABLE (TYPE=4, CODE=0x1)
# Layout: SOF, TYPE, CODE, LEN=0x18 (16 nibbles header + 8 context), dst/src IP header, context payload, EOF

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 4
# CODE (HOST_UNREACHABLE)
networkcore sendtest 1
# LEN (0x18 → 24 nibbles)
networkcore sendtest 1
networkcore sendtest 8
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
# (Router reporting an unreachable host from this address)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
# CONTEXT: unreachable target host IP 192.168.1.50 → 12,0,10,8,0,1,3,2
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 3
networkcore sendtest 2
# EOF
networkcore sendtest 0
