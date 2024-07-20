run: bin
	sbt run

bin: fib.c
	riscv32-unknown-linux-gnu-gcc -S fib.c -march=rv32id
	riscv32-unknown-linux-gnu-gcc -Wl,-Ttext=0x0 -nostdlib -march=rv32i -o fib fib.s
	riscv32-unknown-linux-gnu-objcopy -O binary fib fib.bin

dump: bin
	riscv32-unknown-linux-gnu-objdump -d fib > fib.dump


clean:
	rm -f fib
	rm -f fib.bin
	rm -f fib.dump
