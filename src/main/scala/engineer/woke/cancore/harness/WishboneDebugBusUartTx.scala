package engineer.woke.cancore.harness

import chisel3._
import chisel3.util._

class WishboneDebugBusUartTx(val clockFreq : Int = 250000, val baud : Int = 9600) extends Module {
  val io = IO(new Bundle {
    val ready = Output(Bool())
    val output = Output(Bool())
    val data = Input(UInt(8.W))
    val valid = Input(Bool())
  })
  val clkPerBit = clockFreq / baud
  val counter = RegInit(10.U(log2Up(clkPerBit)))
  val state = RegInit(0.U(4.W))
  val dataSend = Reg(UInt(8.W))
  io.ready := state === 0.U

  when(state === 0.U) {
    when(io.valid) {
      state := 1.U
      dataSend := io.data
      counter := clkPerBit.U
    }.otherwise {
      counter := 10.U
    }
  }.elsewhen(counter === 0.U) {
    when(state === 10.U) {
      state := 0.U
    }
  }.otherwise {
    counter := counter - 1.U
  }

  when(state === 0.U) {
    io.output := true.B
  }.elsewhen(state === 1.U) {
    io.output := false.B
  }.otherwise {
    io.output := dataSend(state - 2.U)
  }
}

