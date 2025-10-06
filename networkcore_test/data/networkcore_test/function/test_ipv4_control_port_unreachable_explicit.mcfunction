# IPv4 Control frame (TYPE=4, CODE=2 PORT_UNREACHABLE) with port 0x002A in context
# Tests explicit IPv4 Control PORT_UNREACHABLE frame parsing (not just triggering the response)
# Layout: SOF, TYPE=4, CODE=2, LEN=0x14 (20 nibbles: 16 addresses + 4 port), DEST_IP, SRC_IP, PORT, EOF

# SOF
networkcore sendtest 15
# TYPE (IPv4 Control)
networkcore sendtest 4
# CODE (PORT_UNREACHABLE)
networkcore sendtest 2
# LEN (0x14 = 20 nibbles)
networkcore sendtest 1
networkcore sendtest 4
# DEST_IP (127.0.0.1)
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
# SRC_IP (192.168.1.1)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
# CONTEXT ARGS (unreachable port 0x002A)
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# EOF
networkcore sendtest 0
