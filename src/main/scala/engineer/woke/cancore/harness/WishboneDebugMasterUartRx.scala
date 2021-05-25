package engineer.woke.cancore.harness

import chisel3._
import chisel3.util._

class WishboneDebugBusUartRx(val clockFreq : Int = 250000, val baud : Int = 9600) extends Module {
  val io = IO(new Bundle {
    val in = Input(Bool())
    val data = Output(UInt(8.W))
    val valid = Output(Bool())
  })
  val clkPerBit = clockFreq / baud
  val clkPerBitAndAHalf = (3*clkPerBit) / 2
  val counter = RegInit(10.U(log2Up(clkPerBit)))
  val state = RegInit(0.U(4.W))
  val valid = RegInit(false.B)
  io.valid := valid
  val data = Reg(Vec(8,Bool()))
  io.data := data

  when(state === 0.U ) {
    when(io.in) {
      state := 1.U
    }.otherwise {
      valid := false.B
      counter := 10.U
    }
  }.elsewhen(counter === 0.U) {
    when(state === 9.U) {
      when(io.in) {
        valid := true.B
      }.otherwise {
        valid := false.B
      }
      state := 0.U
    }.otherwise {
      valid := false.B
      state := state + 1.U
      data(state - 1.U) := io.in
      counter := clkPerBit.U
    }
  }.otherwise {
    counter := counter - 1.U
  }
}

