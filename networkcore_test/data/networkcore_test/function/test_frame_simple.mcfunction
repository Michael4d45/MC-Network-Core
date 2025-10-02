# Simple data frame with no payload
# SOF=15, TYPE=0, DST_WORLD_HI=0, DST_WORLD_LO=0, DST_PORT_HI_HI=0, DST_PORT_HI_LO=0, DST_PORT_LO_HI=2, DST_PORT_LO_LO=10, SRC_WORLD_HI=0, SRC_WORLD_LO=0, SRC_PORT_HI_HI=0, SRC_PORT_HI_LO=0, SRC_PORT_LO_HI=1, SRC_PORT_LO_LO=2, LEN_HI=0, LEN_LO=0, EOF=0
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 0
# DST_WORLD
networkcore sendtest 0
networkcore sendtest 0
# DST_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# SRC_WORLD
networkcore sendtest 0
networkcore sendtest 0
# SRC_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
# LEN
networkcore sendtest 0
networkcore sendtest 0
# EOF
networkcore sendtest 0
