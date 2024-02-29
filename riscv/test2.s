# riscv64-linux-gnu-as -march=rv32i test2.s -o test2.o
# riscv64-linux-gnu-objcopy -O ihex test2.o test2.hex
# riscv64-linux-gnu-objcopy -O binary test2.o test2.bin

li x1, 0x12345678
# lui x1, 74565
# addi x1, x1, 1656
nop
nop
nop
sw x1, 0(x0)
nop
lw x2, 0(x0)