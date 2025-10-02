# IPv4 frame from external host with payload [10,11]
# SOF=15 (Start of Frame), TYPE=3 (IPv4), DST_IP=7,15,0,0,0,0,0,1 (127.0.0.1), DST_UDP_PORT=3,0,3,9 (12345), DST_WORLD=0,0 (0), DST_PORT=0,0,2,10 (42), SRC_IP=12,0,10,8,0,1,0,10 (192.168.1.10), SRC_UDP_PORT=0,0,0,0 (0), SRC_WORLD=0,0 (0), SRC_PORT=0,0,1,2 (18), LEN=0,2 (2), 10, 11 (Payload), EOF=0 (End of Frame)
# DST_PORT = 42, SRC_IP = 192.168.1.10, SRC_PORT = 18, PAYLOAD = [10, 11]
networkcore pauseTickProcess
# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 3
# DST_IP
networkcore sendtest 7
networkcore sendtest 15
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 1
# DST_UDP_PORT
networkcore sendtest 3
networkcore sendtest 0
networkcore sendtest 3
networkcore sendtest 9
# DST_WORLD
networkcore sendtest 0
networkcore sendtest 0
# DST_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 2
networkcore sendtest 10
# SRC_IP
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
# SRC_UDP_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
# SRC_WORLD
networkcore sendtest 0
networkcore sendtest 0
# SRC_PORT
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
networkcore sendtest 0
# LEN
networkcore sendtest 0
networkcore sendtest 2
# PAYLOAD
networkcore sendtest 10
networkcore sendtest 11
# EOF
networkcore sendtest 0
networkcore resumeTickProcess
