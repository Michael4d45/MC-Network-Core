# Control RESET frame (flush TX/RX, clear errors)
# SOF=15 (Start of Frame), TYPE=1 (Control), OP=1 (RESET), LEN_HI=0, LEN_LO=0 (Length=0), EOF=0 (End of Frame)
# RESET = flush TX/RX, clear errors
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
