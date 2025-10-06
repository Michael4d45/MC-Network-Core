# Max length data frame test with 247 nibble payload (LEN=0xFF for 255 total args)
# Layout: SOF, TYPE=0, CODE=0, LEN=0xFF, DST_PORT=0x002A, SRC_PORT=0x0012, PAYLOAD(247 nibbles), EOF
# LEN field now represents TOTAL ARGS (8 port nibbles + 247 payload = 255)
# Payload pattern: repeated helper cycle 1..14 (17 times) + tail 1..9 → 238 + 9 = 247 nibbles

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 0
# CODE
networkcore sendtest 0
# LEN (0xFF → 255 total args = 8 ports + 247 payload)
networkcore sendtest 15
networkcore sendtest 15
# DST_PORT (0x002A)
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# SRC_PORT (0x0012)
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
# PAYLOAD (247 nibbles)
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
function networkcore_test:payload_cycle_14
function networkcore_test:payload_cycle_14
# Tail 9 nibbles (total 247)
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
