package engineer.woke.cancore

import chisel3._

//VERIFIED CORRECT TO can_register_asyn.v VIA YOSYS EQUIV ON 4/27/2021, DONT FUCKING TOUCH
class CanRegisterAsyncReset(width : Int, resetValue : Int) extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    val dataIn : UInt = Input(UInt(width.W))
    val dataOut : UInt = Output(UInt(width.W))
    val writeEn : Bool = Input(Bool())
  })
  val dataOut : UInt = RegInit(resetValue.U(width.W))
  io.dataOut := dataOut
  when(io.writeEn) {
    dataOut := io.dataIn
  }
}

object CanRegisterAsyncReset {
  def apply(resetValue: Int, dataIn: UInt, writeEn: Bool): UInt = {
    val m = Module(new CanRegisterAsyncReset(dataIn.getWidth,resetValue))
    m.io.dataIn := dataIn
    m.io.writeEn := writeEn
    m.io.dataOut
  }
}