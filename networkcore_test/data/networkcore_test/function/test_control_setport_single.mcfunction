# Control SETPORT frame with 1 nibble (port 7)
# SOF=15 (Start of Frame), TYPE=1 (Control), LEN_HI=0, LEN_LO=2 (Length=2), OP=4 (SETPORT), ARG[0]=7 (Port=7), EOF=0 (End of Frame)
# PORT = 7
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 4
networkcore sendtest 7
networkcore sendtest 0
