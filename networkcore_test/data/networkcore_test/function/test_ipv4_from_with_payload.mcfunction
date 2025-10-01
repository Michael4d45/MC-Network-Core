# From IPv4 frame with payload [10,11]
# SOF=15 (Start of Frame), TYPE=4 (From IPv4), DST_WORLD_HI=0, DST_WORLD_LO=0 (World=0), DST_PORT_HI=3, DST_PORT_LO=4 (Port=52), SRC_IP_N0=C, SRC_IP_N1=0, SRC_IP_N2=A, SRC_IP_N3=8, SRC_IP_N4=0, SRC_IP_N5=1, SRC_IP_N6=0, SRC_IP_N7=A (IP=192.168.1.10), SRC_PORT_HI=1, SRC_PORT_LO=2 (Port=18), LEN_HI=0, LEN_LO=2 (Length=2), 10, 11 (Payload), EOF=0 (End of Frame)
# DST_PORT = 52, SRC_IP = 192.168.1.10, SRC_PORT = 18, PAYLOAD = [10, 11] 
networkcore pauseTickProcess
networkcore sendtest 15
networkcore sendtest 4
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 4
networkcore sendtest 12
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 8
networkcore sendtest 0
networkcore sendtest 1
networkcore sendtest 0
networkcore sendtest 10
networkcore sendtest 1
networkcore sendtest 2
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
networkcore sendtest 11
networkcore sendtest 0
networkcore resumeTickProcess
