# IPv4 frame (from 192.168.1.25) carrying an encapsulated DataFrame with 219-nibble payload
# Outer LEN=0xFF (255 args), inner DataFrame LEN=0xE3 (ports + payload)

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 3
# CODE
networkcore sendtest 0
# LEN (0xFF)
networkcore sendtest 15
networkcore sendtest 15
# DST_IP (192.168.1.161)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 10
networkcore sendtest 1
# DST_UDP_PORT (0x3039)
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
# SRC_IP (192.168.1.25)
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 1
networkcore sendtest 9
# SRC_UDP_PORT (0x3039)
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
# ENCAPSULATED FRAME HEADER (TYPE=0 DataFrame, CODE=0, LEN=0xE3)
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 14
networkcore sendtest 3
# ENCAPSULATED DST_PORT=0x0000, SRC_PORT=0x002A
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# PAYLOAD: 15 helper cycles (210 nibbles) + tail 1..9 = 219
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
# Tail 9 nibbles
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 5
networkcore sendtest 6
networkcore sendtest 7
networkcore sendtest 8
networkcore sendtest 9
# EOF
networkcore sendtest 0
