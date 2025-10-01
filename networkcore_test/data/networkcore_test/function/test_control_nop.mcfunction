# Control NOP frame (idle/resync)
# SOF=15 (Start of Frame), TYPE=1 (Control), LEN_HI=0, LEN_LO=1 (Length=1), OP=0 (NOP), EOF=0 (End of Frame)
# NOP = idle/resync assist
networkcore sendtest 15
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 0
