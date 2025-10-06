# Status frame (TYPE=2) example: signature, port=0x002A, RX depth=3, errorFlags=0x5
# Layout: SOF, TYPE=2, CODE=0, LEN=0x07, signature/port/depth/error, EOF

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 2
# CODE
networkcore sendtest 0
# LEN (0x07)
networkcore sendtest 0
networkcore sendtest 7
# SIGNATURE (0xA)
networkcore sendtest 10
# PORT (0x002A)
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# RX DEPTH (3)
networkcore sendtest 3
# ERROR FLAGS (0x5)
networkcore sendtest 5
# EOF
networkcore sendtest 0
