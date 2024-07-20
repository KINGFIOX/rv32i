fib.bin: fib.c
	riscv64-unknown-linux-gnu-gcc -S fib.c
	riscv64-unknown-linux-gnu-gcc -Wl,-Ttext=0x0 -nostdlib -march=rv64i -mabi=lp64 -o fib fib.s
	riscv64-unknown-linux-gnu-objcopy -O binary fib fib.bin

clean:
	rm -f fib
	rm -f fib.bin
