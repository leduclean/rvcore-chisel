    .section .text,"ax",@progbits
    .globl _start

_start:
    # On met la pile à la fin de la mémoire
    la   sp, _memory_end
    la   t0, isr
    csrw mtvec, t0
    # mise à zéro de bss
    la   t0, _bss_start
    la   t1, _bss_end
1:  sw   zero, 0(t0)
    addi t0, t0, 4
    blt  t0, t1, 1b
    # appel de main, sans espoir de retour !
    jal  main
1:  j 1b

    .weak handler

isr :
    # sauve les registres scratch (non sauvés) par le compilo
    # lors d'appels de fonctions
    addi  sp, sp, -16 * 4
    sw    ra,  0*4(sp)
    sw    a0,  1*4(sp)
    sw    a1,  2*4(sp)
    sw    a2,  3*4(sp)
    sw    a3,  4*4(sp)
    sw    a4,  5*4(sp)
    sw    a5,  6*4(sp)
    sw    a6,  7*4(sp)
    sw    a7,  8*4(sp)
    sw    t0,  9*4(sp)
    sw    t1, 10*4(sp)
    sw    t2, 11*4(sp)
    sw    t3, 12*4(sp)
    sw    t4, 13*4(sp)
    sw    t5, 14*4(sp)
    sw    t6, 15*4(sp)

    # appelle le traitant avec ses 3 paramètres
    csrr    a0, mcause
    csrr    a1, mip
    csrr    a2, mie
    jal     handler

    # rétablit les registres avec leur valeur originale
    lw    ra,  0*4(sp)
    lw    a0,  1*4(sp)
    lw    a1,  2*4(sp)
    lw    a2,  3*4(sp)
    lw    a3,  4*4(sp)
    lw    a4,  5*4(sp)
    lw    a5,  6*4(sp)
    lw    a6,  7*4(sp)
    lw    a7,  8*4(sp)
    lw    t0,  9*4(sp)
    lw    t1, 10*4(sp)
    lw    t2, 11*4(sp)
    lw    t3, 12*4(sp)
    lw    t4, 13*4(sp)
    lw    t5, 14*4(sp)
    lw    t6, 15*4(sp)
    addi sp, sp, 16 * 4
    mret
