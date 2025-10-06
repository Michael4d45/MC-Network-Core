# IPv4 frame encapsulating a Status frame (TYPE=2, CODE=0)
# This tests IPv4 delivery of status information
# LEN = 0x1F (24 addressing + 4 inner header + 7 status payload = 35 nibbles)

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 3
# CODE
networkcore sendtest 0
# LEN (0x23 → 35 nibbles)
networkcore sendtest 2
networkcore sendtest 3
# DST_IP 127.0.0.1
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
# DST_UDP_PORT 0x3039
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
# SRC_IP 192.168.1.1
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
# SRC_UDP_PORT 0x0050
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 5
networkcore sendtest 0
# Inner header: TYPE=2 (Status), CODE=0, LEN=0x07
networkcore sendtest 2
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 7
# Status payload: signature 0xA, port 0x0042, rxDepth=3, errorFlags=0
networkcore sendtest 10
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 4
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 0
# EOF
networkcore sendtest 0
