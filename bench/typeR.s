# expected: FFFFFFFF, FFFFFFFF, FFFFFFFF, 80000000, 00000000, 7FFFFFFF, FFFFFFFF, 80000000, FFFFFFFF, 00000000, 80000000, 00000001, 40000000, 80000000, C0000000, 80000000

# Test for TypeR instructions.
#
# We use signed number on 32 bits so the extremums
# values are (+0x7FFFFFFF, - 0x80000000)

# No bypass implemented yet, so insert a `nop` to wait one cycle
# and ensure the previous instruction's result is ready before use.

    .text

    # We set up extremums values on x1 and x2.
    lui x1, 0x80000
    lui x2, 0x80000
    nop
    addi x1, x1, -1
    nop


    # --- ADD ---
    add x31, x1, x2
    add x31, x2, x1

    # --- SUB ---
    sub x31, x1, x2
    sub x31, x0, x2

    # --- AND ---
    and x31, x1, x2
    and x31, x1, x1

    # --- OR ---
    or x31, x1, x2
    or x31, x2, x2

    # --- XOR ---
    xor x31, x1, x2
    xor x31, x1, x1

    # --- SLL ---
    addi x3, x0, 1
    nop
    sll x31, x3, x1
    sll x31, x3, x0

    # --- SRL ---
    srl x31, x2, x3
    srl x31, x2, x0

    # --- SRA ---
    sra x31, x2, x3
    sra x31, x2, x0