package engineer.woke.cancore

import chisel3._

//VERIFIED CORRECT TO can_register_asyn_syn.v VIA YOSYS EQUIV ON 4/27/2021, DONT FUCKING TOUCH
class CanRegisterAsyncAndSyncReset(width : Int, resetValue : Int) extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    val resetSync : Bool = Input(Bool())
    val dataIn : UInt = Input(UInt(width.W))
    val dataOut : UInt = Output(UInt(width.W))
    val writeEn : Bool = Input(Bool())
  })
  val dataOut : UInt = RegInit(resetValue.U(width.W))
  io.dataOut := dataOut
  when(io.resetSync) {
    dataOut := resetValue.U(width.W)
  }.elsewhen(io.writeEn) {
      dataOut := io.dataIn
  }
}

object CanRegisterAsyncAndSyncReset {
  def apply(resetValue: Int, resetSync : Bool, dataIn: UInt, writeEn: Bool): UInt = {
    val m = Module(new CanRegisterAsyncAndSyncReset(dataIn.getWidth,resetValue))
    m.io.dataIn := dataIn
    m.io.writeEn := writeEn
    m.io.resetSync := resetSync
    m.io.dataOut
  }
}