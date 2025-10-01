# Control SETPORT frame with 4 nibbles (port 42)
# SOF=15 (Start of Frame), TYPE=1 (Control), LEN_HI=0, LEN_LO=3 (Length=3), OP=4 (SETPORT), ARG[0]=2, ARG[1]=10 (Port=42), EOF=0 (End of Frame)
# PORT = 42
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 2
networkcore sendtest 10
networkcore sendtest 0
