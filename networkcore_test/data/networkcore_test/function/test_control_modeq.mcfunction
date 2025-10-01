# Control MODEQ frame (request status)
# SOF=15 (Start of Frame), TYPE=1 (Control), LEN_HI=0, LEN_LO=1 (Length=1), OP=2 (MODEQ), EOF=0 (End of Frame)
# MODEQ = request status
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 0
