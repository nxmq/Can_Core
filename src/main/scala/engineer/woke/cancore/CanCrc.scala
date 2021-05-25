package engineer.woke.cancore

import chisel3._
import chisel3.util.Cat

//VERIFIED CORRECT TO can_acf.v VIA YOSYS EQUIV ON 4/27/2021, DONT FUCKING TOUCH
class CanCrc extends Module with RequireSyncReset {
  val io = IO(new Bundle {
    val data : Bool = Input(Bool())
    val enable : Bool = Input(Bool())
    val crc : UInt = Output(UInt(15.W))
  })
  val crc : UInt = RegInit(0.U(15.W))
  val crcNext : Bool = io.data ^ io.crc(14)
  val crcTmp : UInt = Cat(io.crc(13,0),false.B)
  io.crc := crc

  when(io.enable) {
    when(crcNext) {
      crc := crcTmp ^ 0x4599.U
    }.otherwise {
      crc := crcTmp
    }
  }
}

