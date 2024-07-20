package rv32i

import emu.Settings

class CPU(irom: Array[Byte]) {

  val regs: Array[Long] = Array.fill(32)(0) // 我自己约束是 u32

  val csrs: Array[Long] = Array.fill(4096)(0) // 有 4096 个 csr

  var pc: Long = 0 // 地址

  regs(2) = Settings.DRAM_BEGIN + Settings.DRAM_LEN // 设置 sp 为栈顶(高地址)

  val bus = new BUS(irom)

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

  def load(addr: Long, size: Int): Long = {
    bus.load(addr, size)
  }

  def store(addr: Long, size: Int, value: Long): Unit = {
    bus.store(addr, size, value)
  }

  def fetch(): Long = {
    bus.load(pc, 32)
  }

  /** @param inst
    * @return
    *   true : 成功, false : 失败
    */
  def execute(inst: Long): Boolean = {
    val opcode = inst & 0x7f
    val rd     = ((inst >> 7) & 0x1f).toInt
    val rs1    = ((inst >> 15) & 0x1f).toInt
    val rs2    = ((inst >> 20) & 0x1f).toInt
    val funct3 = (inst >> 12) & 0x7
    val funct7 = (inst >> 25) & 0x7f

    opcode match {
      case 0x03 => // load
        val imm  = ((inst >> 20).toInt << 20) >> 20
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 => regs(rd) = load(addr, 8).toByte.toLong // lbu
          case 0x1 => regs(rd) = load(addr, 16).toShort.toLong // lhu
          case 0x4 => regs(rd) = load(addr, 8)
          case 0x5 => regs(rd) = load(addr, 16)
          case 0x2 => regs(rd) = load(addr, 32) // lw
          case _ => return false
        }
      case 0x23 => // 0b010_0011 -> store
        val imm  = ((inst >> 25) << 5) | ((inst >> 7) & 0x1f)
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 => store(addr, 8, regs(rs2))
          case 0x1 => store(addr, 16, regs(rs2))
          case 0x2 => store(addr, 32, regs(rs2))
          case _ => return false
        }
      case 0x13 => // alu imm
        val imm   = ((inst >> 20).toInt << 20) >> 20
        val shamt = (imm & 0x3f).toInt // 0b0011_1111
        funct3 match {
          case 0x0 => regs(rd) = regs(rs1) + imm
          case 0x1 => regs(rd) = regs(rs1) << shamt
          case 0x2 => regs(rd) = if (regs(rs1) < imm) 1 else 0
          case 0x3 => regs(rd) = if (regs(rs1) < imm) 1 else 0
          case 0x4 => regs(rd) = regs(rs1) ^ imm
          case 0x6 => regs(rd) = regs(rs1) | imm
          case 0x7 => regs(rd) = regs(rs1) & imm
          case 0x5 =>
            if ((funct7 >> 1) == 0x00) regs(rd) = regs(rs1) >>> shamt // srli
            else regs(rd)                       = (regs(rs1).toInt >> shamt).toLong // srai
          case _ => return false
        }
      case 0x17 => // 0b001_0111 -> auipc
        val imm = (inst & 0xfffff000L) >> 12
        regs(rd) = pc + (imm << 12)
      case 0x33 => // 0b011_0011 -> alu
        val shamt = (regs(rs2) & 0x3f).toInt
        (funct3, funct7) match {
          case (0x0, 0x00) => regs(rd) = regs(rs1) + regs(rs2)
          case (0x0, 0x20) => regs(rd) = regs(rs1) - regs(rs2)
          case (0x1, 0x00) => regs(rd) = regs(rs1) << shamt
          case (0x2, 0x00) => regs(rd) = if (regs(rs1) < regs(rs2)) 1 else 0
          case (0x3, 0x00) => regs(rd) = if (regs(rs1) < regs(rs2)) 1 else 0
          case (0x4, 0x00) => regs(rd) = regs(rs1) ^ regs(rs2)
          case (0x5, 0x00) => regs(rd) = regs(rs1) >>> shamt
          case (0x5, 0x20) => regs(rd) = (regs(rs1).toInt >> shamt).toLong
          case (0x6, 0x00) => regs(rd) = regs(rs1) | regs(rs2)
          case (0x7, 0x00) => regs(rd) = regs(rs1) & regs(rs2)
          case _ => return false
        }
      case 0x37 => regs(rd) = (inst & 0x0_ffff_f000L) // 0b011_0111 -> lui
      case 0x63 => // branch
        val imm = (((inst >> 31) << 12) | ((inst >> 25) << 5) | ((inst >> 8) & 0x0f) | ((inst >> 7) & 0x01))
        funct3 match {
          case 0x0 => if (regs(rs1) == regs(rs2)) pc += imm - 4
          case 0x1 => if (regs(rs1) != regs(rs2)) pc += imm - 4
          case 0x4 => if (regs(rs1) < regs(rs2)) pc += imm - 4
          case 0x5 => if (regs(rs1) >= regs(rs2)) pc += imm - 4
          case 0x6 => if (regs(rs1) < regs(rs2)) pc += imm - 4
          case 0x7 => if (regs(rs1) >= regs(rs2)) pc += imm - 4
          case _ => return false
        }
      case 0x67 => // 0b110_0111 -> jalr
        val imm = ((inst >> 20).toInt << 20) >> 20
        val t   = pc
        pc       = (regs(rs1) + imm) & ~1
        regs(rd) = t
      case 0x6f => // 0b110_1111 -> jal
        val imm = (((inst >> 31) << 20) | ((inst >> 12) & 0xff) | ((inst >> 20) & 0x01) | ((inst >> 21) & 0x3ff)) << 1
        regs(rd) = pc + 4
        pc += imm - 4
      case _ =>
        println(f"not implemented yet: opcode $opcode%#x")
        return false
    }
    true
  }

}
