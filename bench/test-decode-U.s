# expected: 17B00000,37100037
# this file aims to test if decoding of U-immediate instructions works (step 2)
# it does not aim to understand the sematics of RISC-V instructions.

    .text

    auipc x11, 0
    lui x1, 33