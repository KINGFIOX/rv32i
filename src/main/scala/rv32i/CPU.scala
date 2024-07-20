package rv32i

import emu.Settings

class CPU(irom: Array[Byte]) {

  val regs: Array[Int] = Array.fill(32)(0) // 我自己约束是 u32

  val csrs: Array[Int] = Array.fill(4096)(0) // 有 4096 个 csr

  val abi = Seq(
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

  var pc: Int = 0 // 地址

  regs(2) = Settings.DRAM_BEGIN + Settings.DRAM_LEN // 设置 sp 为栈顶(高地址)

  val bus = new BUS(irom)

  // 打印所有寄存器的值
  def dumpRegisters(): Unit = {
    for (i <- 0 until 32 by 4) {
      println(s"${abi(i)}=${regs(i)}\t${abi(i + 1)}=${regs(i + 1)}\t${abi(i + 2)}=${regs(i + 2)}\t${abi(i + 3)}=${regs(i + 3)}\t")
    }
  }

  def load(addr: Int, size: Int): Int = {
    bus.load(addr, size)
  }

  def store(addr: Int, size: Int, value: Int): Unit = {
    bus.store(addr, size, value)
  }

  def fetch(): Int = {
    bus.load(pc, 32)
  }

  /** @param inst
    * @return
    *   true : 成功, false : 失败
    */
  def execute(inst: Int): Boolean = {
    val opcode = inst & 0x7f
    val rd     = (inst >>> 7) & 0x1f
    val rs1    = (inst >>> 15) & 0x1f
    val rs2    = (inst >>> 20) & 0x1f
    val funct3 = (inst >>> 12) & 0x7
    val funct7 = (inst >>> 25) & 0x7f

    regs(0) = 0 // 开始前清 0

    opcode match {
      case 0x03 => // load
        val imm  = inst >> 20 /* 这个是有符号的 */
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 =>
            println(s"lbu ${abi(rd)}, $imm(${abi(rs1)})")
            regs(rd) = load(addr, 8).toByte
          case 0x1 =>
            println(s"lhu ${abi(rd)}, $imm(${abi(rs1)})")
            regs(rd) = load(addr, 16).toShort
          case 0x4 =>
            println(s"lbu ${abi(rd)}, $imm(${abi(rs1)})")
            regs(rd) = load(addr, 8)
          case 0x5 =>
            println(s"lhu ${abi(rd)}, $imm(${abi(rs1)})")
            regs(rd) = load(addr, 16)
          case 0x2 =>
            println(s"lw ${abi(rd)}, $imm(${abi(rs1)})")
            regs(rd) = load(addr, 32)
          case _ => return false
        }
      case 0x23 => // 0b010_0011 -> store
        val imm  = ((inst >> 25) << 5) | ((inst >> 7) & 0x1f)
        val addr = regs(rs1) + imm
        funct3 match {
          case 0x0 =>
            println(s"sb ${abi(rs2)}, $imm(${abi(rs1)})")
            store(addr, 8, regs(rs2))
          case 0x1 =>
            println(s"sh ${abi(rs2)}, $imm(${abi(rs1)})")
            store(addr, 16, regs(rs2))
          case 0x2 =>
            println(s"sw ${abi(rs2)}, $imm(${abi(rs1)})")
            store(addr, 32, regs(rs2))
          case _ => return false
        }
      case 0x13 => // alu imm
        val imm   = inst >> 20
        val shamt = imm & 0x3f // 0b0011_1111
        funct3 match {
          case 0x0 =>
            println(s"addi ${abi(rd)}, ${abi(rs1)}, $imm")
            regs(rd) = regs(rs1) + imm
          case 0x1 =>
            println(s"slli ${abi(rd)}, ${abi(rs1)}, $shamt")
            regs(rd) = regs(rs1) << shamt
          case 0x2 =>
            println(s"slti ${abi(rd)}, ${abi(rs1)}, $imm")
            regs(rd) = if (regs(rs1) < imm) 1 else 0
          case 0x3 =>
            println(s"sltiu ${abi(rd)}, ${abi(rs1)}, $imm")
            regs(rd) = if (regs(rs1) < imm) 1 else 0
          case 0x4 =>
            println(s"xori ${abi(rd)}, ${abi(rs1)}, $imm")
            regs(rd) = regs(rs1) ^ imm
          case 0x6 =>
            println(s"ori ${abi(rd)}, ${abi(rs1)}, $imm")
            regs(rd) = regs(rs1) | imm
          case 0x7 =>
            println(s"andi ${abi(rd)}, ${abi(rs1)}, $imm")
            regs(rd) = regs(rs1) & imm
          case 0x5 =>
            if ((funct7 >> 1) == 0x00) {
              println(s"srli ${abi(rd)}, ${abi(rs1)}, $shamt")
              regs(rd) = regs(rs1) >>> shamt
            } else {
              println(s"srai ${abi(rd)}, ${abi(rs1)}, $shamt")
              regs(rd) = (regs(rs1) >> shamt)
            }
          case _ => return false
        }
      case 0x17 => // 0b001_0111 -> auipc
        val imm = (inst & 0xfffff000) >> 12
        println(s"auipc ${abi(rd)}, $imm")
        regs(rd) = pc + (imm << 12)
      case 0x33 => // 0b011_0011 -> alu
        val shamt = regs(rs2) & 0x3f
        (funct3, funct7) match {
          case (0x0, 0x00) =>
            println(s"add ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = regs(rs1) + regs(rs2)
          case (0x0, 0x20) =>
            println(s"sub ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = regs(rs1) - regs(rs2)
          case (0x1, 0x00) =>
            println(s"sll ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = regs(rs1) << shamt
          case (0x2, 0x00) =>
            println(s"slt ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = if (regs(rs1) < regs(rs2)) 1 else 0
          case (0x3, 0x00) =>
            println(s"sltu ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = if (regs(rs1) < regs(rs2)) 1 else 0
          case (0x4, 0x00) =>
            println(s"xor ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = regs(rs1) ^ regs(rs2)
          case (0x5, 0x00) =>
            println(s"srl ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = regs(rs1) >>> shamt
          case (0x5, 0x20) =>
            println(s"sra ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = (regs(rs1) >> shamt)
          case (0x6, 0x00) =>
            println(s"or ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = regs(rs1) | regs(rs2)
          case (0x7, 0x00) =>
            println(s"and ${abi(rd)}, ${abi(rs1)}, ${abi(rs2)}")
            regs(rd) = regs(rs1) & regs(rs2)
          case _ => return false
        }
      case 0x37 =>
        println(s"lui ${abi(rd)}, ${inst & 0x0_ffff_f000}")
        regs(rd) = inst & 0x0_ffff_f000 // 0b011_0111 -> lui
      case 0x63 => // branch
        val imm = (((inst >> 31) << 12) | ((inst >> 25) << 5) | ((inst >> 8) & 0x0f) | ((inst >> 7) & 0x01)) << 1
        funct3 match {
          case 0x0 =>
            println(s"beq ${abi(rs1)}, ${abi(rs2)}, $imm")
            if (regs(rs1) == regs(rs2)) pc += imm - 4
          case 0x1 =>
            println(s"bne ${abi(rs1)}, ${abi(rs2)}, $imm")
            if (regs(rs1) != regs(rs2)) pc += imm - 4
          case 0x4 =>
            println(s"blt ${abi(rs1)}, ${abi(rs2)}, $imm")
            if (regs(rs1) < regs(rs2)) pc += imm - 4
          case 0x5 =>
            println(s"bge ${abi(rs1)}, ${abi(rs2)}, $imm")
            if (regs(rs1) >= regs(rs2)) pc += imm - 4
          case 0x6 =>
            println(s"bltu ${abi(rs1)}, ${abi(rs2)}, $imm")
            if (regs(rs1) < regs(rs2)) pc += imm - 4
          case 0x7 =>
            println(s"bgeu ${abi(rs1)}, ${abi(rs2)}, $imm")
            if (regs(rs1) >= regs(rs2)) pc += imm - 4
          case _ => return false
        }
      case 0x67 => // 0b110_0111 -> jalr
        val imm = inst >> 20
        val t   = pc
        println(s"jalr ${abi(rd)}, ${abi(rs1)}, $imm")
        pc       = (regs(rs1) + imm) & ~1
        regs(rd) = t
      case 0x6f => // 0b110_1111 -> jal
        val imm = (((inst >> 31) << 20) | ((inst >> 12) & 0xff) | ((inst >> 20) & 0x01) | ((inst >> 21) & 0x3ff)) << 1
        println(s"jal ${abi(rd)}, $imm")
        regs(rd) = pc + 4
        pc += imm - 4
      case _ =>
        println(f"not implemented yet: opcode $opcode%#x")
        return false
    }

    regs(0) = 0 // 结束时清零

    true
  }

}
