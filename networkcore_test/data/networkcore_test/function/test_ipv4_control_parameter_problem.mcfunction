# IPv4 Control frame: PARAMETER_PROBLEM (TYPE=4, CODE=0x5)
# Layout: SOF, TYPE, CODE, LEN=0x10 (16 nibbles header, no additional context), dst/src IP header, EOF

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 4
# CODE (PARAMETER_PROBLEM)
networkcore sendtest 5
# LEN (0x10 → 16 nibbles)
networkcore sendtest 1
networkcore sendtest 0
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
# (Router reporting malformed frame from this address)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
# EOF
networkcore sendtest 0
