# riscv64-linux-gnu-as -march=rv32i fib.s -o fib.o
# riscv64-linux-gnu-objcopy -O ihex fib.o fib.hex
# riscv64-linux-gnu-objcopy -O binary fib.o fib.bin

fib_init:
    li x16, 0x100
    li x1, 1 # temp value 1
    li x2, 1 # temp value 2
    li x4, 0 # counter
    li x8, 0 # the greatest seen x3
fib_start:
    add x3, x1, x2
    mv x1, x2
    mv x2, x3
    bgt x8, x3, fib_end # end if overflow (found biggest fib number that fits in 32 bits)
    sw x3, 0x100(x4)
    addi x8, x3, 0
    addi x4, x4, 1
    j fib_start
fib_end:
    sw x4, 0xFF(x0)
    j fib_init
