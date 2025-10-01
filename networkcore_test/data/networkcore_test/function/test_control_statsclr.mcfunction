# Control STATSCLR frame (clear counters)
# SOF=15 (Start of Frame), TYPE=1 (Control), LEN_HI=0, LEN_LO=1 (Length=1), OP=5 (STATSCLR), EOF=0 (End of Frame)
# STATSCLR = clear counters
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 5
networkcore sendtest 0
