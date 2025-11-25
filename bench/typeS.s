# expected: 00000078, 00005678, 12345678

    .text

    la x1, x
    la x2, a
    lw x2, 0(x2)
    sb x2, 0(x1)
    lb x31, 0(x1)
    sh x2, 0(x1)
    lh x31, 0(x1)
    sw x2, 0(x1)
    lw x31, 0(x1)

    .data
a: .word 0x12345678
x: .word 0
