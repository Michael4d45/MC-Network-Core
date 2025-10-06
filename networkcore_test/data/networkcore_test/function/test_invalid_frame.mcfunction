# Invalid data frame (LEN=0x08 but missing EOF)
# Layout: SOF, TYPE=0, CODE=0, LEN=0x08, ports, stray nibble instead of EOF
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 0
# CODE
networkcore sendtest 0
# LEN (0x08 → ports only)
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
# Invalid: missing EOF, stray nibble follows
networkcore sendtest 1
