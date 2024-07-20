package emu

import rv32i.CPU

import java.io.{File, FileInputStream, IOException}
import scala.io.Source
import scala.util.{Try, Using}

object Settings {
  val DRAM_BEGIN: Long = 0 // 0x0_8000_0000
  val DRAM_END: Long   = 0x0_ffff_f000L
  def DRAM_LEN         = DRAM_END - DRAM_BEGIN

  val DIG_BEGIN: Long = 0x0_ffff_f000L //
  val DIG_LEN: Long   = 4

  val LED_BEGIN: Long = 0x0_ffff_f060L
  val LED_LEN: Long   = 3

  val SWITCH_BEGIN: Long = 0x0_ffff_f070L
  val SWITCH_LEN: Long   = 3

  val BTN_BEGIN: Long = 0x0_ffff_f078L
  val BTN_LEN: Long   = 1
}

object Main {
  def main(args: Array[String]): Unit = {
    val fileName = if (args.length != 1) "meminit.bin" else args(0)

    val code = readFile(fileName) match {
      case Right(bytes) => bytes
      case Left(error) =>
        println(s"Error reading file: $error")
        return
    }

    val cpu = new CPU(code)

    while (true) {
      // 1. Fetch.
      val inst = cpu.fetch()

      // Break the loop if an error occurs.
      if (inst == 0) return

      // 2. Add 4 to the program counter.
      cpu.pc += 4

      // 3. Decode.
      // 4. Execute.
      if (cpu.execute(inst) == false) return

      // This is a workaround for avoiding an infinite loop.
      if (cpu.pc == 0) return
    }

    cpu.dumpRegisters()
  }

  def readFile(filename: String): Either[Throwable, Array[Byte]] = {
    Using(new FileInputStream(new File(filename))) { inputStream =>
      val buffer = new Array[Byte](inputStream.available())
      inputStream.read(buffer)
      buffer
    }.toEither
  }
}
