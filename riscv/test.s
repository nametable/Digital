# riscv64-linux-gnu-as -march=rv32i test.s -o test.o
# riscv64-linux-gnu-objcopy -O ihex test.o test.hex

start:

    addi x1, x0, 0x12
    sll x1, x1, 8
    addi x1, x1, 0x34

    mv x2, x1
    sub x3, x2, x1 # x3 should be 0
    sll x4, x1, 16 # x4 should be 0x12340000

    li x10, -1 # x10 should be 0xffffffff

    j start
