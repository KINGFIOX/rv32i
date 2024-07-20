package rv32i

class DRAM(code: Array[Byte], val DRAM_BASE: Long, val DRAM_SIZE: Long /* 大小 */ ) {
  val dram: Array[Byte] = Array.fill(DRAM_SIZE.toInt)(0)
  Array.copy(code, 0, dram, 0, code.length) // 初始化内存

  def load(addr: Long, size: Long): Long = {
    val index = (addr - DRAM_BASE).toInt // dram 的 index
    size match {
      case 8 => { dram(index).toLong & 0xff }
      case 16 => { (dram(index).toLong & 0xff) | ((dram(index + 1).toLong & 0xff) << 8) }
      case 32 => { (dram(index).toLong & 0xff) | ((dram(index + 1).toLong & 0xff) << 8) | ((dram(index + 2).toLong & 0xff) << 16) | ((dram(index + 3).toLong & 0xff) << 24) }
      case _ => 0
    }
  }

  def store(addr: Long, size: Long, value: Long): Unit = {
    val index = (addr - DRAM_BASE).toInt // dram 的 index
    size match {
      case 8 => { dram(index) = (value & 0xff).toByte }
      case 16 => {
        dram(index)     = (value & 0xff).toByte
        dram(index + 1) = ((value >> 8) & 0xff).toByte
      }
      case 32 => {
        dram(index)     = (value & 0xff).toByte
        dram(index + 1) = ((value >> 8) & 0xff).toByte
        dram(index + 2) = ((value >> 16) & 0xff).toByte
        dram(index + 3) = ((value >> 24) & 0xff).toByte
      }
      case _ =>
    }
  }
}
