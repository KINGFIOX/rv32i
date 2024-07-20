package rv32i

import emu.Settings

class BUS(code: Array[Byte]) {
  val dram = new DRAM(code, Settings.DRAM_BEGIN, Settings.DRAM_LEN)

  def load(addr: Long, size: Long): Long = {
    // if (Settings.DRAM_BEGIN <= addr) dram.load(addr, size) else 0
    addr match {
      case a: Long if Settings.DRAM_BEGIN <= a && a < Settings.DRAM_END =>
        dram.load(a, size)
      //   case a: Long if Settings.BTN_BEGIN <= a && a < Settings.BTN_BEGIN + Settings.BTN_LEN => /* 访问按钮 */ dram.load(a, size)
      case _: Long => 0
    }
  }

  def store(addr: Long, size: Long, value: Long): Unit = {
    // if (Settings.DRAM_BEGIN <= addr) dram.store(addr, size, value)
    addr match {
      case a: Long if Settings.DRAM_BEGIN <= a && a < Settings.DRAM_END => dram.store(addr, size, value)
      case _: Long => /* 啥也不干 */
    }
  }
}
