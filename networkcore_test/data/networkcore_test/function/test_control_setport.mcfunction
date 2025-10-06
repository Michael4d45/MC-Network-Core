# Control SETPORT frame requesting port 42 (0x2A)
# SOF=15, TYPE=1, CODE=8 (SETPORT), LEN=0x04, payload=port nibbles, EOF=0
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 4
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
networkcore sendtest 0
