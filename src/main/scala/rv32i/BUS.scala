package rv32i

class BUS(code: Array[Byte], val DRAM_BASE: Long, val DRAM_SIZE: Long) {
  val dram = new DRAM(code, DRAM_BASE, DRAM_SIZE)

  def load(addr: Long, size: Long): Long = {
    if (DRAM_BASE <= addr) dram.load(addr, size) else 0
  }

  def store(addr: Long, size: Long, value: Long): Unit = {
    if (DRAM_BASE <= addr) dram.store(addr, size, value)
  }
}
