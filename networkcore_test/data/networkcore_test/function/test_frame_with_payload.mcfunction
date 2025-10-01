# Frame with payload [10,11]
# SOF=15, TYPE=0, DST_WORLD_HI=0, DST_WORLD_LO=0, DST_PORT_HI_HI=0, DST_PORT_HI_LO=0, DST_PORT_LO_HI=3, DST_PORT_LO_LO=4, SRC_WORLD_HI=0, SRC_WORLD_LO=0, SRC_PORT_HI_HI=0, SRC_PORT_HI_LO=0, SRC_PORT_LO_HI=1, SRC_PORT_LO_LO=2, LEN_HI=0, LEN_LO=2, 10, 11, EOF=0
networkcore pauseTickProcess
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 0
networkcore resumeTickProcess
