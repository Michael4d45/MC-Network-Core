# IPv4 frame to external host with encapsulated DataFrame payload [10,11]
# Layout: SOF, TYPE=3, CODE=0, LEN=0x26, addressing, inner frame header LEN=0x0A, ports + payload, EOF
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 3
# CODE
networkcore sendtest 0
# LEN (0x26 → 24 address + 4 header + 10 inner args)
networkcore sendtest 2
networkcore sendtest 6
# DST_IP (192.168.1.10)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 10
# DST_UDP_PORT (0x0034)
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 4
# SRC_IP (127.0.0.1)
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
# SRC_UDP_PORT (0x3039)
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
# ENCAPSULATED FRAME HEADER (TYPE=0, CODE=0, LEN=0x0A)
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 10
# ENCAPSULATED ARGS: DST/SRC PORTS + PAYLOAD
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 10
networkcore sendtest 11
# EOF
networkcore sendtest 0
