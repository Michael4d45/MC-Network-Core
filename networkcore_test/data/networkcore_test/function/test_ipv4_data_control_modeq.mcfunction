# IPv4 frame encapsulating a Data Control MODEQ (code 0x6) targeting port 0x0042
# LEN = 0x1C (28 nibbles: addressing + inner header)
networkcore sendtest 15
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 12
# DST_IP 192.168.1.10
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 10
# DST_UDP_PORT 0x0042
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 4
networkcore sendtest 2
# SRC_IP 127.0.0.1
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
# SRC_UDP_PORT 0x3039
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
# Inner header: TYPE=1 (DataControl), CODE=6 (MODEQ), LEN=0x00
networkcore sendtest 1
networkcore sendtest 6
networkcore sendtest 0
networkcore sendtest 0
# EOF
networkcore sendtest 0
