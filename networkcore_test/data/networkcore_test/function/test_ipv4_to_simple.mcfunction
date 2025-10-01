# Simple To IPv4 frame with no payload
# SOF=15 (Start of Frame), TYPE=3 (To IPv4), SRC_WORLD_HI=0, SRC_WORLD_LO=0 (World=0), SRC_PORT_HI_HI=0, SRC_PORT_HI_LO=0, SRC_PORT_LO_HI=1, SRC_PORT_LO_LO=2 (Port=18), DST_IP_N0=C, DST_IP_N1=0, DST_IP_N2=A, DST_IP_N3=8, DST_IP_N4=0, DST_IP_N5=1, DST_IP_N6=0, DST_IP_N7=A (IP=192.168.1.10), DST_PORT_HI_HI=0, DST_PORT_HI_LO=0, DST_PORT_LO_HI=3, DST_PORT_LO_LO=4 (Port=52), LEN_HI=0, LEN_LO=0 (Length=0), EOF=0 (End of Frame)
# SRC_PORT = 18, DST_IP = 192.168.1.10, DST_PORT = 52, PAYLOAD = []
networkcore sendtest 15
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
