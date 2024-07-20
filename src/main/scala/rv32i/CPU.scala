package rv32i

import emu.Settings

class CPU(irom: Array[Byte]) {

  val regs: Array[Long] = Array.fill(32)(0) // 我自己约束是 u32

  val csrs: Array[Long] = Array.fill(4096)(0) // 有 4096 个 csr

  var pc: Long = 0 // 地址

  regs(2) = Settings.DRAM_BASE + Settings.DRAM_SIZE

  // 打印所有寄存器的值
  def dumpRegisters(): Unit = {
    val abi = Array(
      "zero",
      "ra",
      "sp",
      "gp",
      "tp",
      "t0",
      "t1",
      "t2",
      "s0",
      "s1",
      "a0",
      "a1",
      "a2",
      "a3",
      "a4",
      "a5",
      "a6",
      "a7",
      "s2",
      "s3",
      "s4",
      "s5",
      "s6",
      "s7",
      "s8",
      "s9",
      "s10",
      "s11",
      "t3",
      "t4",
      "t5",
      "t6"
    )
    println(s"========== dump ==========")
    for (i <- 0 until 32 by 4) {
      println(s"${abi(i)}=${regs(i)}\t${abi(i + 1)}=${regs(i + 1)}\t${abi(i + 2)}=${regs(i + 2)}\t${abi(i + 3)}=${regs(i + 3)}\t")
    }
  }

  // 注意一下: scala 存取的都是 Int
  // def load(addr: Int, size: Int): Int /* 返回的值 */ = {}

  // def store(addr: Long, size: Int, value: Int) = {}

  // def fetch(addr: Long) = {}

  def execute(inst: Long): Boolean = {
    val opcode = inst & 0x7f
    val rd     = ((inst >> 7) & 0x1f).toInt
    val rs1    = ((inst >> 15) & 0x1f).toInt
    val rs2    = ((inst >> 20) & 0x1f).toInt
    val funct3 = (inst >> 12) & 0x7
    val funct7 = (inst >> 25) & 0x7f

    // 模拟寄存器x0被硬连到0
    regs(0) = 0
    true
  }

}
