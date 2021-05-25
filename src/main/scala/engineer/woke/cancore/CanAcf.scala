package engineer.woke.cancore

import chisel3.util._
import chisel3._

//VERIFIED CORRECT TO can_acf.v VIA YOSYS EQUIV ON 4/27/2021, DONT FUCKING TOUCH
class CanAcf extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    val id : UInt = Input(UInt(29.W))
    val resetMode : Bool = Input(Bool())
    val acceptanceFilterMode : Bool = Input(Bool())
    val extendedMode : Bool = Input(Bool())
    val acceptanceCode : Vec[UInt] = Input(Vec(4,UInt(8.W)))
    val acceptanceMask : Vec[UInt] = Input(Vec(4,UInt(8.W)))
    val goRxCrcLim : Bool = Input(Bool())
    val goRxInter : Bool = Input(Bool())
    val goErrorFrame : Bool = Input(Bool())
    val data0 : UInt = Input(UInt(8.W))
    val data1 : UInt = Input(UInt(8.W))
    val rtr1 : Bool = Input(Bool())
    val rtr2 : Bool = Input(Bool())
    val ide : Bool = Input(Bool())
    val noByte0 : Bool = Input(Bool())
    val noByte1 : Bool = Input(Bool())
    val idOk : Bool = Output(Bool())
  })
  val idOk : Bool = RegInit(false.B)
  io.idOk := idOk

  def cmpWithMask(a : UInt,b : UInt,m : UInt) : Bool = {
    ((~(a ^ b)).asUInt() | m).andR()
  }
    val idMatch : Bool = cmpWithMask(io.id(10,3), io.acceptanceCode(0), io.acceptanceMask(0)) // PROVED OK

  val matchSfStd : Bool = idMatch &
    cmpWithMask(Cat(io.id(2,0),io.rtr1),io.acceptanceCode(1)(7,4), io.acceptanceMask(1)(7,4)) &
    (cmpWithMask(io.data0, io.acceptanceCode(2), io.acceptanceMask(2)) | io.noByte0) &
    (cmpWithMask(io.data1, io.acceptanceCode(3), io.acceptanceMask(3)) | io.noByte1)  // PROVED OK

  val matchSfExt : Bool = cmpWithMask(io.id(28,21), io.acceptanceCode(0), io.acceptanceMask(0)) &
    cmpWithMask(io.id(20,13), io.acceptanceCode(1), io.acceptanceMask(1)) &
    cmpWithMask(io.id(12,5), io.acceptanceCode(2), io.acceptanceMask(2)) &
    cmpWithMask(Cat(io.id(4,0),io.rtr2),io.acceptanceCode(3)(7,2),io.acceptanceMask(3)(7,2)) // PROVED OK

  
  val matchDfStd : Bool = (idMatch &
    cmpWithMask(Cat(io.id(2,0),io.rtr1),io.acceptanceCode(1)(7,4),io.acceptanceMask(1)(7,4)) &
    (cmpWithMask(io.data0(7,4), io.acceptanceCode(1)(3,0), io.acceptanceMask(1)(3,0)) | io.noByte0) &
    (cmpWithMask(io.data0(3,0), io.acceptanceCode(3)(3,0), io.acceptanceMask(3)(3,0)) | io.noByte0)) |
    (cmpWithMask(io.id(10,3), io.acceptanceCode(2), io.acceptanceMask(2)) &
    cmpWithMask(Cat(io.id(2,0),io.rtr1),io.acceptanceCode(3)(7,4),io.acceptanceMask(3)(7,4))) // PROVED OK

  val matchDfExt : Bool = (cmpWithMask(io.id(28,21), io.acceptanceCode(0), io.acceptanceMask(0)) &
                     cmpWithMask(io.id(20,13), io.acceptanceCode(1), io.acceptanceMask(1))) |
                    (cmpWithMask(io.id(28,21), io.acceptanceCode(2), io.acceptanceMask(2)) &
                     cmpWithMask(io.id(20,13), io.acceptanceCode(3), io.acceptanceMask(3))) // PROVED OK

  when(io.goRxCrcLim) {
    idOk := Mux(io.extendedMode,Mux(io.acceptanceFilterMode,Mux(io.ide,matchSfExt,matchSfStd),Mux(io.ide,matchDfExt,matchDfStd)),idMatch) // PROVED OK
  }.elsewhen(io.resetMode | io.goRxInter | io.goErrorFrame) {
    idOk := false.B // PROVED OK
  }
}
