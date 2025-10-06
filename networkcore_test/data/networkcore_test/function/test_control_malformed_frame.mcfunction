# Data Control MALFORMED_FRAME notification (code 0x2) with no args
# SOF=15, TYPE=1, CODE=2, LEN=0x00, EOF=0
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
