# Control SETPORT frame with 4 nibbles (port 42)
# SOF=15 (Start of Frame), TYPE=1 (Control), OP=3 (SETPORT), LEN_HI=0, LEN_LO=2 (Length=2), ARG[0]=2, ARG[1]=10 (Port=42), EOF=0 (End of Frame)
# PORT = 42
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 1
# OP
networkcore sendtest 3
# LEN
networkcore sendtest 0
networkcore sendtest 2
# ARGS
networkcore sendtest 2
networkcore sendtest 10
# EOF
networkcore sendtest 0
