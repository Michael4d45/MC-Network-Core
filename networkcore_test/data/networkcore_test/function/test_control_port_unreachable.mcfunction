# Data Control PORT_UNREACHABLE frame (code 0x1) reporting port 0x1234
# SOF=15, TYPE=1, CODE=1, LEN=0x04, ARG[0..3]=0x1,0x2,0x3,0x4, EOF=0
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 4
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 0
