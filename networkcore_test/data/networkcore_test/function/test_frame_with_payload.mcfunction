# Data frame with payload [10,11]
# Layout: SOF, TYPE=0, CODE=0, LEN(0x0A), DST_PORT=0x002A, SRC_PORT=0x0012, PAYLOAD=0x0A 0x0B, EOF
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 0
# CODE
networkcore sendtest 0
# LEN (0x0A → ports + 2 payload nibbles)
networkcore sendtest 0
networkcore sendtest 10
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
# PAYLOAD
networkcore sendtest 10
networkcore sendtest 11
# EOF
networkcore sendtest 0
