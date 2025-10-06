# Simple data frame (TYPE=0, CODE=0) with no payload
# Layout: SOF, TYPE, CODE, LEN(0x08), DST_PORT=0x002A, SRC_PORT=0x0012, EOF
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 0
# CODE
networkcore sendtest 0
# LEN (0x08 → dst/src ports only)
networkcore sendtest 0
networkcore sendtest 8
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
# EOF
networkcore sendtest 0
