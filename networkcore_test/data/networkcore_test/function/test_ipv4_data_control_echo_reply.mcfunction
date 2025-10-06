# IPv4 frame encapsulating a Data Control ECHO_REPLY (code 0x5) with payload [0xA,0xB]
# LEN = 0x1E (30 nibbles)
networkcore sendtest 15
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 14
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
# Inner header: TYPE=1, CODE=5, LEN=0x02
networkcore sendtest 1
networkcore sendtest 5
networkcore sendtest 0
networkcore sendtest 2
# Payload [0xA, 0xB]
networkcore sendtest 10
networkcore sendtest 11
# EOF
networkcore sendtest 0
