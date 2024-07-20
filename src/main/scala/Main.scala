package emu

import rv32i.CPU

import java.io.{File, FileInputStream, IOException}
import scala.io.Source
import scala.util.{Try, Using}

object Settings {
  val DRAM_BASE: Long = 0x0_8000_0000
  val DRAM_SIZE: Long = 1024 * 1024 * 128
}

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage:rv32i-emu <filename>")
      return
    }

    val filename = args(0)
    val code = readFile(filename) match {
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
      if (cpu.execute(inst)) return

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
