# expected: 00000004, 00000005, 0000000D
    .text

    addi x31, x0, 4
    jalr x2, 8(x31)     # x2 = 8
    addi x31, x31, 1    # should be skipped
    addi x31, x31, 1    # x31 = 5
    add x31, x2, x31   # x31 = 13
