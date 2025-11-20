# expected: 00000001, 00000001, 00000002, 00000002, 00000003, 00000003, 00000004, 00000004, 00000005, 00000005, 00000006, 00000006


# Test for TypeB instructions.
# For each branch instruction, we test a satisfied condition
# and an unsatisfied condition.

    .text
    # Init values for comp
    addi x1, x0, 1   # x1 = 1
    addi x2, x0, 1   # x2 = 1
    sub x3, x0, x1   # x3 = -1 -> signed(-1) / unsigned(`b11111` = 31)


    # --- BEQ ---
    beq x1, x2, skip1
    lui x31, 0xfffff
    lui x31, 0x00000
    skip1:
    addi x31, x0, 1

    beq x1, x3, skip1b
    addi x31, x0, 1       # should execute because x1 != x3
    skip1b:

    # --- BNE ---
    bne x3, x1, skip2
    lui x31, 0xfffff
    lui x31, 0x00000
    skip2:
    addi x31, x31, 1

    bne x1, x2, skip2b
    addi x31, x0, 2       # should execute because x1 == x2
    skip2b:

    # --- BLT ---
    blt x3, x1, skip3
    lui x31, 0xfffff
    lui x31, 0x00000
    skip3:
    addi x31, x31, 1

    blt x1, x3, skip3b
    addi x31, x0, 3       # should execute because x1 >= x3 signed
    skip3b:

    # --- BGE ---
    bge x1, x3, skip4
    lui x31, 0xfffff
    lui x31, 0x00000
    skip4:
    addi x31, x31, 1

    bge x3, x1, noskip4
    addi x31, x0, 4       # should execute because x3 < x1 signed
    noskip4:

    # --- BLTU ---
    bltu x1, x3, skip5
    lui x31, 0xfffff
    lui x31, 0x00000
    skip5:
    addi x31, x31, 1

    bltu x3, x1, noskip5
    addi x31, x0, 5       # should execute because x3(31) >= x1(1) unsigned
    noskip5:

    # --- BGEU ---
    bgeu x3, x1, skip6
    lui x31, 0xfffff
    lui x31, 0x00000
    skip6:
    addi x31, x31, 1

    bgeu x1, x3, noskip6
    addi x31, x0, 6       # should execute because x1(1) < x3(31) unsigned
    noskip6:
