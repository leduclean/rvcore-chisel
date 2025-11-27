# expected: FFFFFFFF, 12345678, FFFFFFFF, 0, 0, 0

# This file test the whole csr instruction stack
# To only read data, we will use rs1 = x0
# To only write data, we will use rd = x0

    .text
    # set up
    li x1, 0xFFFFFFFF
    li x2, 0x12345678

    # Read/Write registers (mstatus, mie, mtvec)

    csrrw x0, mstatus, x1
    csrrw x31, mstatus, x0   # x31 = x1

    csrrw x0, mie, x2
    csrrw x31, mie, x0       # x31 = x2

    csrrw x0, mtvec, x1
    csrrw x31, mtvec, x0     # x31 = x1


    # Those registers are read-only (mip, mcause, mepc)

    # mip
    csrrw x3, mip, x0
    csrrw x0, mip, x1
    csrrw x4, mip, x0
    sub x31, x4, x3

    # mcause
    csrrw x3, mcause, x0
    csrrw x0, mcause, x2
    csrrw x4, mcause, x0
    sub x31, x4, x3

    # mepc
    csrrw x3, mepc, x0
    csrrw x0, mepc, x1
    csrrw x4, mepc, x0
    sub x31, x4, x3
