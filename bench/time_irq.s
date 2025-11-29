# expected: FFFFFFFF, 0
    .include "bench/crt_irq.S"

    .section .text
    .global _start

    # This test aims to rapid test Machine Time interupt
        main:
            # Init
            # Timer settings
            .equ timer_cmp_low, 0x02004000
            li t1, timer_cmp_low

            # mtip at cycle 20
            li t0, 20
            sw t0, 0(t1)

            # Global interupt enable
            # Timer settings
            csrwi	mstatus, 8

            # Machine time interupt enable
            li t0, 0x80
            csrs mie, t0

            # This is cycle 20
            # so an irq is fired
            # and we jump to handler

            j after  # C12 -> mtip

    after:
        addi x31, x0, 0


    handler:
        addi x31, x0, -1
        # We disable mtie
        li t0, 0x80
        csrc mie, t0
        # MRET -> get back to previous state which is
        # after
        mret
