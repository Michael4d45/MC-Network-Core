# Control SETPER frame (set symbol period to 4)
# SOF=15 (Start of Frame), TYPE=1 (Control), LEN_HI=0, LEN_LO=2 (Length=2), OP=3 (SETPER), ARG[0]=4 (Period=4), EOF=0 (End of Frame)
# PERIOD = 4 ticks
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 0
