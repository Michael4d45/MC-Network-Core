# Data Control RESET frame (code 0x7)
# Standalone test for RESET control frame
# SOF=15, TYPE=1, CODE=7, LEN=0x00 (no args), EOF=0

# SOF
networkcore sendtest 15
# TYPE
networkcore sendtest 1
# CODE (RESET)
networkcore sendtest 7
# LEN (0x00 → no args)
networkcore sendtest 0
networkcore sendtest 0
# EOF
networkcore sendtest 0
