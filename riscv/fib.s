# riscv64-linux-gnu-as -march=rv32i fib.s -o fib.o
# riscv64-linux-gnu-objcopy -O ihex fib.o fib.hex
# riscv64-linux-gnu-objcopy -O binary fib.o fib.bin

    li x1, 1
    li x2, 1
    li x4, 32
fib_start:
    add x3, x1, x2
    mv x1, x2
    mv x2, x3
    addi x4, x4, -1
    beq x4, x0, fib_end
    j fib_start
fib_end:
    sw x3, 0x0(x0)
    j fib_end
