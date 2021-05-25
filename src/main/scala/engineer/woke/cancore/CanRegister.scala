package engineer.woke.cancore

import chisel3._

//VERIFIED CORRECT TO can_register.v VIA YOSYS EQUIV ON 4/27/2021, DONT FUCKING TOUCH
class CanRegister(width : Int) extends Module {
  val io = IO(new Bundle {
    val dataIn : UInt = Input(UInt(width.W))
    val dataOut : UInt = Output(UInt(width.W))
    val writeEn : Bool = Input(Bool())
  })

  val dataOut : UInt = RegInit(0.U(width.W))
  io.dataOut := dataOut
  when(io.writeEn) {
    dataOut := io.dataIn
  }

}


object CanRegister {
  def apply(dataIn: UInt, writeEn: Bool): UInt = {
    val m = Module(new CanRegister(dataIn.getWidth))
    m.io.dataIn := dataIn
    m.io.writeEn := writeEn
    m.io.dataOut
  }
}