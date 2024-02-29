// riscv64-linux-gnu-gcc -ffreestanding -nostdlib -march=rv32i -mabi=ilp32 -c test.c -o testc.o
// riscv64-linux-gnu-objcopy -O ihex testc.o testc.hex
// riscv64-linux-gnu-gcc -ffreestanding -nostdlib -march=rv32i -mabi=ilp32 -S -fverbose-asm -02 test.c
// riscv64-linux-gnu-gcc -ffreestanding -nostdlib -march=rv32i -mabi=ilp32 -S -fverbose-asm testc.c
// https://www.sifive.com/blog/all-aboard-part-1-compiler-args
void main() {
    int a = 1;
    int b = 2;
    int c = a + b;
}