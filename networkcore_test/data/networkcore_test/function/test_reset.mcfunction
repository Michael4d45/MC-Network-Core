# Control RESET frame (flush TX/RX, clear errors)
# SOF=15 (Start of Frame), TYPE=1 (Control), LEN_HI=0, LEN_LO=1 (Length=1), OP=1 (RESET), EOF=0 (End of Frame)
# RESET = flush TX/RX, clear errors
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 1
networkcore sendtest 0
