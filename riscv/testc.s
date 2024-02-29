	.file	"testc.c"
	.option pic
	.attribute arch, "rv32i2p1"
	.attribute unaligned_access, 0
	.attribute stack_align, 16
# GNU C17 (GCC) version 12.2.0 (riscv64-linux-gnu)
#	compiled by GNU C version 12.2.0, GMP version 6.2.1, MPFR version 4.1.0-p13, MPC version 1.2.1, isl version isl-0.26-GMP

# warning: GMP header version 6.2.1 differs from library version 6.3.0.
# warning: MPFR header version 4.1.0-p13 differs from library version 4.2.1.
# warning: MPC header version 1.2.1 differs from library version 1.3.1.
# GGC heuristics: --param ggc-min-expand=100 --param ggc-min-heapsize=131072
# options passed: -mabi=ilp32 -misa-spec=20191213 -march=rv32i -ffreestanding
	.text
	.align	2
	.globl	main
	.type	main, @function
main:
	addi	sp,sp,-32	#,,
	sw	s0,28(sp)	#,
	addi	s0,sp,32	#,,
# testc.c:6:     int a = 1;
	li	a5,1		# tmp72,
	sw	a5,-20(s0)	# tmp72, a
# testc.c:7:     int b = 2;
	li	a5,2		# tmp73,
	sw	a5,-24(s0)	# tmp73, b
# testc.c:8:     int c = a + b;
	lw	a4,-20(s0)		# tmp75, a
	lw	a5,-24(s0)		# tmp76, b
	add	a5,a4,a5	# tmp76, tmp74, tmp75
	sw	a5,-28(s0)	# tmp74, c
# testc.c:9: }
	nop	
	lw	s0,28(sp)		#,
	addi	sp,sp,32	#,,
	jr	ra		#
	.size	main, .-main
	.ident	"GCC: (GNU) 12.2.0"
	.section	.note.GNU-stack,"",@progbits
