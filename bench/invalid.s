# expected: 00000000
# works as long as we haven't implemented 

    .text
# reset x31
    lui x31, 0
    sltu x1, x0, x1
