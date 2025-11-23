# expected: FFFFFF80, 0000007F, 0000080, 00000007F, FFFF8000, 00007FFF, 00008000, 00007FFF, 80000000, 00000000,

.text

    # ----- LB -----
    la x1, bytes
    lb x31, 0(x1)
    lb x31, 1(x1)

    # ----- LBU -----
    la x1, bytes
    lbu x31, 0(x1)
    lbu x31, 1(x1)

    # ----- LH -----
    la x2, half
    lh x31, 0(x2)
    lh x31, 2(x2)

    # ----- LHU -----
    la x2, half
    lhu x31, 0(x2)
    lhu x31, 2(x2)

    # ----- LW -----
    la x3, word
    lw x31, 0(x3)
    lw x31, 4(x3)

.data
    bytes: .byte 0x80, 0x7F
    half: .half 0x8000, 0x7FFF

    # Force the word address to be a multiple of 4
    .align 2
    word: .word 0x80000000, 0x00000000
