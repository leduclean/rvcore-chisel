# expected: 000007FF,FFFFF800,FFFFFFFF,00000000,00000000,00000000,00000000,FFFFFFFF,00000000,000007FF,FFFFFFFF,00000000,000007FF,00000001,80000000,FFFFFFFF,00000001,FFFFFFFF,FFFFFFFF,00000001,00000000,00000000,00000001,00000001


# Test for TypeI instructions.
#
# In this test file, we test the maximum value
# for an immediate on 12 bits: (-2048, 2047).


    .text
    # --- ADDI ---
    addi x31, x0, 2047
    addi x31, x0, -2048
    addi x31, x0, -1
    addi x31, x0, 0

    # --- ANDI ---
    andi x31, x0, -1
    andi x31, x0, 0
    andi x31, x0, 2047

    # --- ORI ---
    ori x31, x0, -1
    ori x31, x0, 0
    ori x31, x0, 2047

    # --- XORI ---
    xori x31, x0, -1
    xori x31, x0, 0
    xori x31, x0, 2047

    # --- SLLI ---
    addi x1, x0, 1
    slli x31, x1, 0
    slli x31, x1, 31

    # --- SRLI ---
    addi x2, x0, -1
    srli x31, x2, 0
    srli x31, x2, 31

    # --- SRAI ---
    addi x3, x0, -1
    srai x31, x3, 0
    srai x31, x3, 31

    # --- SLTI ---
    addi x4, x0, 5
    slti x31, x4, 10
    slti x31, x4, -1
    addi x5, x0, 0
    slti x31, x5, -2048

    # --- SLTIU ---
    addi x6, x0, 5
    sltiu x31, x6, 10
    sltiu x31, x6, -1
    addi x7, x0, 0
    sltiu x31, x7, -1