# Control MODEQ frame (request status)
# SOF=15 (Start of Frame), TYPE=1 (Control), OP=2 (MODEQ), LEN_HI=0, LEN_LO=0 (Length=0), EOF=0 (End of Frame)
# MODEQ = request status
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 1
# OP
networkcore sendtest 2
# LEN
networkcore sendtest 0
networkcore sendtest 0
# EOF
networkcore sendtest 0
