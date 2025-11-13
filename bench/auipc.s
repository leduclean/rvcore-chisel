# expected: 00000000,FFFFF004,12345008
	.text
    auipc x31, 0          # Add a null imm to pc
    auipc x31, 0xfffff    # Add max imm to pc
    auipc x31, 0x12345    # Add random value to pc