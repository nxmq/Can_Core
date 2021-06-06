package engineer.woke.cancore

import chisel3._
import chisel3.util.{Cat}

//VERIFIED CORRECT TO can_fifo.v VIA YOSYS EQUIV ON 5/1/2021, DONT FUCKING TOUCH
class CanFifo extends Module with RequireAsyncReset {
  val io = IO(new Bundle {
    val wr : Bool = Input(Bool())
    val dataIn : UInt = Input(UInt(8.W))
    val addr : UInt = Input(UInt(6.W))
    val dataOut : UInt = Output(UInt(8.W))
    val resetMode : Bool = Input(Bool())
    val releaseBuffer : Bool = Input(Bool())
    val extendedMode : Bool = Input(Bool())
    val overrun : Bool = Output(Bool())
    val infoEmpty : Bool = Output(Bool())
    val infoCnt : UInt = Output(UInt(7.W))
  })

  val infoCnt : UInt = RegInit(0.U(7.W))
  io.infoCnt := infoCnt
  val fifo : SyncReadMem[UInt] = SyncReadMem(64,UInt(8.W))
  val lengthFifo : SyncReadMem[UInt] = SyncReadMem(64,UInt(4.W))
  val overrunInfo : SyncReadMem[Bool] = SyncReadMem(64,Bool())

  val rdPointer : UInt = RegInit(0.U(6.W))
  val wrPointer : UInt = RegInit(0.U(6.W))
  val readAddress : UInt = Wire(UInt(6.W))
  val wrInfoPointer : UInt = RegInit(0.U(6.W))
  val rdInfoPointer : UInt = RegInit(0.U(6.W))
  val wrQ : Bool = RegNext(Mux(io.resetMode,false.B,io.wr),false.B)
  val lenCnt : UInt = RegInit(0.U(4.W))
  val fifoCnt : UInt = RegInit(0.U(7.W))

  val latchOverrun : Bool = RegInit(false.B)
  val initializeMemories : Bool = RegInit(true.B)
  val lengthInfo : UInt = Wire(UInt(4.W))
  val writeLengthInfo : Bool = !io.wr & wrQ
  val fifoEmpty : Bool = fifoCnt === 0.U
  val fifoFull : Bool = fifoCnt === 64.U
  val infoFull : Bool = infoCnt === 64.U
  io.infoEmpty := infoCnt === 0.U
  lengthInfo := lengthFifo.read(rdInfoPointer)
  readAddress := (rdPointer + (io.addr - Mux(io.extendedMode,16.U(6.W),20.U(6.W))))

  when(io.resetMode | writeLengthInfo) {
    lenCnt := 0.U
  }.elsewhen(io.wr & (!fifoFull)) {
    lenCnt := lenCnt + 1.U
  }

  when(writeLengthInfo & (!infoFull) | initializeMemories) {
    wrInfoPointer := wrInfoPointer + 1.U
  }.elsewhen(io.resetMode) {
    wrInfoPointer := rdInfoPointer
  }

  when(io.releaseBuffer & !infoFull) {
    rdInfoPointer := rdInfoPointer +& 1.U
  }

  when(io.releaseBuffer & !fifoEmpty) {
    rdPointer := rdPointer + lengthInfo
  }

  when(io.resetMode) {
    wrPointer := rdPointer
  }.elsewhen(io.wr & !fifoFull) {
    wrPointer := wrPointer + 1.U
  }

  when(io.resetMode | writeLengthInfo) {
    latchOverrun := false.B
  }.elsewhen(io.wr & fifoFull) {
    latchOverrun := true.B
  }

  when(io.resetMode) {
    fifoCnt := 0.U
  }.elsewhen(io.wr & !io.releaseBuffer & !fifoFull) {
    fifoCnt := fifoCnt +& 1.U
  }.elsewhen(!io.wr & io.releaseBuffer & !fifoEmpty) {
    fifoCnt := fifoCnt - Cat(0.U(3.W),lengthInfo)
  }.elsewhen(io.wr & io.releaseBuffer & !fifoFull & !fifoEmpty) {
      fifoCnt := fifoCnt - Cat(0.U(3.W),lengthInfo) +& 1.U
  }

  when(io.resetMode) {
    infoCnt := 0.U
  }.elsewhen(writeLengthInfo ^ io.releaseBuffer) {
    when(io.releaseBuffer & !io.infoEmpty) {
      infoCnt := (infoCnt -& 1.U)
    }.elsewhen(writeLengthInfo & !infoFull) {
      infoCnt := (infoCnt +& 1.U)
    }
  }

  when(wrInfoPointer.andR()) {
    initializeMemories := false.B
  }

  when(io.wr & !fifoFull) {
    fifo(wrPointer) := io.dataIn
  }

  io.dataOut := fifo(readAddress)

  when(writeLengthInfo & (!infoFull) | initializeMemories) {
    lengthFifo(wrInfoPointer) := Mux(initializeMemories,  0.U, lenCnt)
  }

  when(writeLengthInfo & (!infoFull) | initializeMemories) {
    overrunInfo.write(wrInfoPointer,(latchOverrun | (io.wr & fifoFull)) & (!initializeMemories))
  }
  io.overrun := overrunInfo(rdInfoPointer)
}
