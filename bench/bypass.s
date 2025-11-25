# expected: 1, 1, 0, 1, 3, 7, F, 0, FFFFFFFE, FFFFFFFF, FFFFFFFF, 0

# Test register dependencies to assert EX/EX and EX/MEM bypass are working

    .text
    # EX/EX dependencies
    addi x31, x0, 1        # x31 = 1
    add x31, x31, x0       # x31 = 1 (EX/EX)
    sub x31, x31, x31      # x31 = 0 (EX/EX)

    addi x31, x0, 1        # x31 = 1
    addi x31, x31, 2       # x31 = 3
    addi x31, x31, 4       # x31 = 7
    addi x31, x31, 8       # x31 = F
    xor  x31, x31, x31     # x31 = 0



    # EX/MEM dependencies
    la x1, var # x1 = addr(var)
    lw x31, 0(x1)      # x31 = var
    addi x31, x31, 1   # x31 = var + 1
    sw x31, 4(x1)      # store var + 1
    lw x31, 4(x1)      # x31 = var + 1
    addi x31, x31, 1   # x31 = var + 2 = 0


    .data
var: .word 0xFFFFFFFE
