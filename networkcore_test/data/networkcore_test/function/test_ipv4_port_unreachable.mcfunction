# IPv4 frame targeting an unbound local port (0xFFEE) to trigger IPv4 PORT_UNREACHABLE
# Encapsulated payload is an empty Data frame.
networkcore sendtest 15
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 4
# DST_IP 192.168.1.10
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 10
# DST_UDP_PORT 0xFFEE
networkcore sendtest 15
networkcore sendtest 15
networkcore sendtest 14
networkcore sendtest 14
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
# Inner header: TYPE=0 (DataFrame), CODE=0, LEN=0x08
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 8
# Inner args: DST_PORT=0xFFEE SRC_PORT=0x1000
networkcore sendtest 15
networkcore sendtest 15
networkcore sendtest 14
networkcore sendtest 14
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
# EOF
networkcore sendtest 0
