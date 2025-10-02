# Control SETPER frame (set symbol period to 4)
# SOF=15 (Start of Frame), TYPE=1 (Control), OP=3 (SETPER), LEN_HI=0, LEN_LO=1 (Length=1), ARG[0]=4 (Period=4), EOF=0 (End of Frame)
# PERIOD = 4 ticks
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 1
# OP
networkcore sendtest 3
# LEN
networkcore sendtest 0
networkcore sendtest 1
# ARG
networkcore sendtest 4
# EOF
networkcore sendtest 0
