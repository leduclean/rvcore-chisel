# expected: 00000001, 00000002, 0000000A

    .text

    addi x31, x0, 1
    jal x2, 12           # x2 = 8
    addi x31, x31, 1    # should be skipped
    addi x31, x31, 1    # x31 = 2
    add x31, x2, x31    # x31 = 10
