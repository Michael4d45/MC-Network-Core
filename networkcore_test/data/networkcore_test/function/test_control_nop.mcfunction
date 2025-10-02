# Control NOP frame (idle/resync)
# SOF=15 (Start of Frame), TYPE=1 (Control), OP=0 (NOP), LEN_HI=0, LEN_LO=0 (Length=0), EOF=0 (End of Frame)
# NOP = idle/resync assist
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 1
# OP
networkcore sendtest 0
# LEN
networkcore sendtest 0
networkcore sendtest 0
# EOF
networkcore sendtest 0
